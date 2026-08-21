package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockRestrictedReleaseEntity;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.api.entity.StockSimPositionEntity;
import com.mx.nqboard.quanta.mapper.StockCandidateMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockRestrictedReleaseMapper;
import com.mx.nqboard.quanta.mapper.StockSimPositionMapper;
import com.mx.nqboard.quanta.screen.ExitEngine;
import com.mx.nqboard.quanta.screen.MarketRegimeCalculator;
import com.mx.nqboard.quanta.screen.ScreenPatternEnum;
import com.mx.nqboard.quanta.service.StockCandidateService;
import com.mx.nqboard.quanta.service.StockScreenResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 候选股票池 服务实现类
 * </p>
 * <p>
 * Gate 硬门在 Java 侧执行（不进 Dify）：
 * H9 解禁门（7 日内解禁占总股本 ≥5% 拒绝）、H10 冷却门（止损出局 10 个交易日内不再入选、
 * 持仓中的股票不再入选）。池内票龄超 5 个交易日未买入自动 EXPIRED。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCandidateServiceImpl extends ServiceImpl<StockCandidateMapper, StockCandidateEntity>
		implements StockCandidateService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String BENCHMARK_INDEX = "sh000300";

	/**
	 * 池内候选有效交易日数（超时未买入自动过期）
	 */
	private static final int CANDIDATE_ACTIVE_DAYS = 5;

	private final StockCandidateMapper stockCandidateMapper;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	private final StockRestrictedReleaseMapper stockRestrictedReleaseMapper;

	private final StockSimPositionMapper stockSimPositionMapper;

	private final StockScreenResultService stockScreenResultService;

	private final MarketRegimeCalculator marketRegimeCalculator;

	/**
	 * 每日入候选池最大数量（yml quanta.screen.top-n）
	 */
	@Value("${quanta.screen.top-n:3}")
	private int topN;

	/**
	 * H9：解禁前瞻天数（日历日）
	 */
	@Value("${quanta.candidate.restricted-release-days:7}")
	private int restrictedReleaseDays;

	/**
	 * H9：解禁占总股本比例下限（%）
	 */
	@Value("${quanta.candidate.restricted-release-min-ratio:5}")
	private double restrictedReleaseMinRatio;

	/**
	 * H10：止损出局冷却天数（日历日，约10个交易日）
	 */
	@Value("${quanta.candidate.cooldown-days:14}")
	private int cooldownDays;

	@Override
	public int refreshCandidates() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		if (latest == null || StrUtil.isBlank(latest.getTradeDate())) {
			throw new IllegalStateException("指数日线无数据，请先执行 index_daily 同步步骤");
		}
		return refreshCandidates(latest.getTradeDate());
	}

	@Override
	public int refreshCandidates(String tradeDate) {
		// 1. 筛选结果 TopN（已通过入池线与大盘门）
		List<StockScreenResultEntity> tops = stockScreenResultService.topCandidates(tradeDate, topN);
		if (CollUtil.isEmpty(tops)) {
			log.info("候选池刷新: 信号日 {} 无通过入池线的候选", tradeDate);
			expireOldCandidates(tradeDate);
			return 0;
		}

		MarketRegimeCalculator.MarketRegime regime = marketRegimeCalculator.calc(tradeDate);
		List<String> tradingDays = recentTradingDays(tradeDate, 2);

		// 2. 过期旧候选（票龄超时未买入）
		expireOldCandidates(tradeDate);

		// 3. 逐只过 Gate 硬门
		List<StockCandidateEntity> candidates = new ArrayList<>();
		for (StockScreenResultEntity top : tops) {
			String reject = gateRejectReason(top.getTsCode(), tradeDate);
			if (reject != null) {
				log.info("候选 {} 被 Gate 拒绝: {}", top.getTsCode(), reject);
				continue;
			}
			StockCandidateEntity entity = buildCandidate(top, tradeDate, regime.ret5d(), tradingDays);
			candidates.add(entity);
		}

		// 4. upsert 落库
		upsertByUniqueKey(tradeDate, candidates);
		log.info("候选池刷新完成: 信号日={}, TopN={}, 入池 {} 只", tradeDate, tops.size(), candidates.size());
		return candidates.size();
	}

	/**
	 * Gate 硬门：H9 解禁 / H10 冷却与持仓去重，返回 null 表示通过
	 */
	private String gateRejectReason(String tsCode, String tradeDate) {
		// H10：持仓中（含未成交委托）不再入选
		Long holding = stockSimPositionMapper.selectCount(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getTsCode, tsCode)
				.in(StockSimPositionEntity::getStatus, "PENDING_BUY", "HOLDING", "PENDING_SELL"));
		if (holding != null && holding > 0) {
			return "H10:持仓中";
		}
		// H10：止损出局冷却期
		String cooldownStart = LocalDate.parse(toIso(tradeDate)).minusDays(cooldownDays).format(BASIC_DATE);
		Long cooldown = stockSimPositionMapper.selectCount(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getTsCode, tsCode)
				.eq(StockSimPositionEntity::getStatus, "EXITED")
				.in(StockSimPositionEntity::getExitReason, "stop_loss", "gap_stop", "breakeven_stop")
				.ge(StockSimPositionEntity::getExitDate, cooldownStart));
		if (cooldown != null && cooldown > 0) {
			return "H10:止损冷却期";
		}
		// H9：近期解禁（注意 float_ratio 为占总股本比例，作为流通市值影响近似）
		String releaseEnd = LocalDate.parse(toIso(tradeDate)).plusDays(restrictedReleaseDays).format(BASIC_DATE);
		List<StockRestrictedReleaseEntity> releases = stockRestrictedReleaseMapper.selectList(
				Wrappers.<StockRestrictedReleaseEntity>lambdaQuery()
						.eq(StockRestrictedReleaseEntity::getTsCode, tsCode)
						.ge(StockRestrictedReleaseEntity::getFloatDate, tradeDate)
						.le(StockRestrictedReleaseEntity::getFloatDate, releaseEnd)
						.isNotNull(StockRestrictedReleaseEntity::getFloatRatio));
		for (StockRestrictedReleaseEntity release : releases) {
			if (release.getFloatRatio().doubleValue() >= restrictedReleaseMinRatio) {
				return "H9:近期解禁" + release.getFloatRatio().doubleValue() + "%";
			}
		}
		return null;
	}

	/**
	 * 组装候选实体（离场计划 + 理由标签从筛选 metrics 提取）
	 */
	private StockCandidateEntity buildCandidate(StockScreenResultEntity top, String tradeDate, double marketRet5d,
			List<String> tradingDays) {
		StockCandidateEntity entity = new StockCandidateEntity();
		entity.setTradeDate(tradeDate);
		entity.setTsCode(top.getTsCode());
		entity.setName(top.getName());
		entity.setScreenScore(top.getScreenScore());
		entity.setPattern(top.getPattern());
		entity.setAction("entry_ok");
		// 置信度映射：65分->82，93分->95 封顶
		double score = top.getScreenScore() != null ? top.getScreenScore().doubleValue() : 65;
		entity.setConfidence((int) Math.min(95, 50 + score * 0.5));
		entity.setMarketRet5d(BigDecimal.valueOf(Math.round(marketRet5d * 1000000) / 1000000.0));
		entity.setStatus("ACTIVE");
		entity.setDecisionMode("rules");
		entity.setExpireDate(expireDate(tradeDate, tradingDays));

		// 理由标签：模板 + 上下文亮点（从 metrics JSON 提取）
		List<String> reasons = new ArrayList<>();
		ScreenPatternEnum pattern = patternOf(top.getPattern());
		if (pattern != null) {
			reasons.add("模板:" + pattern.getLabel());
		}
		if (StrUtil.isNotBlank(top.getMetrics())) {
			try {
				JSONObject metrics = JSON.parseObject(top.getMetrics());
				appendReason(reasons, metrics, "sectorPct", 0.01, "板块共振");
				appendReason(reasons, metrics, "flow3dWan", 0, "主力净流入");
				appendReason(reasons, metrics, "dragonNetWan", 0, "龙虎榜净买");
			}
			catch (Exception ignore) {
				// metrics 解析失败不影响入池
			}
		}
		entity.setReasons(JSON.toJSONString(reasons));

		// 离场计划（价格在买入成交时按开盘价计算，此处存比例与信号日参考价）
		Map<String, Object> exitPlan = new LinkedHashMap<>();
		exitPlan.put("maxHoldDays", ExitEngine.MAX_HOLD_DAYS);
		exitPlan.put("stopLossPct", -ExitEngine.STOP_LOSS_PCT);
		exitPlan.put("takeProfitPct", ExitEngine.TAKE_PROFIT_PCT);
		exitPlan.put("signalDate", tradeDate);
		if (StrUtil.isNotBlank(top.getMetrics())) {
			try {
				JSONObject metrics = JSON.parseObject(top.getMetrics());
				exitPlan.put("signalClose", metrics.getDouble("close"));
			}
			catch (Exception ignore) {
			}
		}
		exitPlan.put("rules", "止损/止盈以收盘价为准触发、次日开盘成交；持有满5个交易日无条件离场；开盘急杀直接卖出");
		entity.setExitPlan(JSON.toJSONString(exitPlan));
		return entity;
	}

	private void appendReason(List<String> reasons, JSONObject metrics, String key, double min, String label) {
		Double v = metrics.getDouble(key);
		if (v != null && v > min) {
			reasons.add(label);
		}
	}

	private ScreenPatternEnum patternOf(String code) {
		for (ScreenPatternEnum p : ScreenPatternEnum.values()) {
			if (p.getCode().equals(code)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * 过期日 = 信号日后第 5 个交易日（交易日历不可用时退化为 +7 日历日）
	 */
	private String expireDate(String tradeDate, List<String> tradingDays) {
		int idx = tradingDays.indexOf(tradeDate);
		if (idx >= 0 && idx + CANDIDATE_ACTIVE_DAYS < tradingDays.size()) {
			return tradingDays.get(idx + CANDIDATE_ACTIVE_DAYS);
		}
		return LocalDate.parse(toIso(tradeDate)).plusDays(CANDIDATE_ACTIVE_DAYS + 2).format(BASIC_DATE);
	}

	/**
	 * 过期旧候选：票龄超 10 个日历日仍未买入 -> EXPIRED
	 */
	private void expireOldCandidates(String tradeDate) {
		String cutoff = LocalDate.parse(toIso(tradeDate)).minusDays(CANDIDATE_ACTIVE_DAYS + 5).format(BASIC_DATE);
		List<StockCandidateEntity> stale = list(Wrappers.<StockCandidateEntity>lambdaQuery()
				.eq(StockCandidateEntity::getStatus, "ACTIVE")
				.lt(StockCandidateEntity::getTradeDate, cutoff));
		if (!stale.isEmpty()) {
			stale.forEach(c -> c.setStatus("EXPIRED"));
			updateBatchById(stale);
			log.info("候选池过期 {} 只（票龄超时）", stale.size());
		}
	}

	/**
	 * 近 N 个交易日（含 tradeDate，升序），向前多取用于过期日推算
	 */
	private List<String> recentTradingDays(String tradeDate, int lookback) {
		// 取信号日之后无法预知，过期日推算取信号日前后窗口：向前 10 + 向后由调用场景退化处理
		List<String> days = stockIndexDailyMapper.selectList(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.le(StockIndexDailyEntity::getTradeDate, tradeDate)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit " + (lookback + 10)))
				.stream()
				.map(StockIndexDailyEntity::getTradeDate)
				.collect(Collectors.toList());
		java.util.Collections.reverse(days);
		return days;
	}

	private void upsertByUniqueKey(String tradeDate, List<StockCandidateEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return;
		}
		Map<String, StockCandidateEntity> existMap = list(Wrappers.<StockCandidateEntity>lambdaQuery()
				.select(StockCandidateEntity::getId, StockCandidateEntity::getTsCode)
				.eq(StockCandidateEntity::getTradeDate, tradeDate))
				.stream()
				.collect(Collectors.toMap(StockCandidateEntity::getTsCode, e -> e));
		List<StockCandidateEntity> toInsert = new ArrayList<>();
		List<StockCandidateEntity> toUpdate = new ArrayList<>();
		for (StockCandidateEntity row : rows) {
			StockCandidateEntity exist = existMap.get(row.getTsCode());
			if (exist == null) {
				toInsert.add(row);
			}
			else {
				row.setId(exist.getId());
				toUpdate.add(row);
			}
		}
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert, 100);
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, 100);
		}
	}

	private String toIso(String basicDate) {
		return basicDate.length() == 8
				? basicDate.substring(0, 4) + "-" + basicDate.substring(4, 6) + "-" + basicDate.substring(6, 8)
				: basicDate;
	}

}
