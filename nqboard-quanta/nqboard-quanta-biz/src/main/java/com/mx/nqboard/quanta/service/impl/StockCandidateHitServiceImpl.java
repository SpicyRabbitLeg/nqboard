package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import com.mx.nqboard.quanta.api.entity.StockCandidateHitEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.mapper.StockCandidateHitMapper;
import com.mx.nqboard.quanta.mapper.StockCandidateMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.screen.ExitEngine;
import com.mx.nqboard.quanta.service.StockCandidateHitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 候选池信号命中率追踪 服务实现类
 * </p>
 * <p>
 * 试运行观察体系核心：信号质量追踪（不依赖是否实际模拟买入，对每个 entry_ok 候选
 * 独立计算前向收益）、LLM vs 规则 A/B 对比（decision_mode 维度聚合）、
 * 分数区间校准（与回测分桶校准互为印证）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCandidateHitServiceImpl extends ServiceImpl<StockCandidateHitMapper, StockCandidateHitEntity>
		implements StockCandidateHitService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String BENCHMARK_INDEX = "sh000300";

	private final StockCandidateHitMapper stockCandidateHitMapper;

	private final StockCandidateMapper stockCandidateMapper;

	private final StockDailyMapper stockDailyMapper;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	/**
	 * 命中率回看信号日数（yml quanta.hit-rate.lookback-days）
	 */
	@Value("${quanta.hit-rate.lookback-days:15}")
	private int lookbackDays;

	@Override
	public int refreshHits() {
		String latest = latestTradeDate();
		if (StrUtil.isBlank(latest)) {
			throw new IllegalStateException("指数日线无数据，请先执行 index_daily 同步步骤");
		}
		return refreshHits(latest);
	}

	@Override
	public int refreshHits(String tradeDate) {
		// 回看窗口内交易日（升序），多取 8 天缓冲保证窗口头部信号有完整前向数据
		List<String> days = recentTradingDays(tradeDate, lookbackDays + 8);
		int processed = 0;
		// 仅处理窗口内的信号日（留尾部 1 天起：至少要有次日才能入场）
		for (int idx = 0; idx < days.size() - 1; idx++) {
			String signalDay = days.get(idx);
			List<StockCandidateEntity> candidates = stockCandidateMapper.selectList(
					Wrappers.<StockCandidateEntity>lambdaQuery()
							.eq(StockCandidateEntity::getTradeDate, signalDay)
							.eq(StockCandidateEntity::getAction, "entry_ok"));
			if (CollUtil.isEmpty(candidates)) {
				continue;
			}
			List<StockCandidateHitEntity> rows = new ArrayList<>();
			for (StockCandidateEntity candidate : candidates) {
				StockCandidateHitEntity hit = computeHit(candidate, days, idx);
				if (hit != null) {
					rows.add(hit);
				}
			}
			processed += upsertByUniqueKey(signalDay, rows);
		}
		log.info("命中率追踪刷新完成: 最新交易日={}, 处理 {} 条候选", tradeDate, processed);
		return processed;
	}

	@Override
	public List<StockCandidateHitEntity> daily(String tradeDate) {
		return list(Wrappers.<StockCandidateHitEntity>lambdaQuery()
				.eq(StockCandidateHitEntity::getTradeDate, tradeDate)
				.orderByDesc(StockCandidateHitEntity::getScreenScore));
	}

	@Override
	public Map<String, Object> summary(int days) {
		String latest = latestTradeDate();
		int window = days > 0 ? days : 30;
		String cutoff = LocalDate.parse(toIso(StrUtil.blankToDefault(latest,
				LocalDate.now().format(BASIC_DATE)))).minusDays((long) (window * 1.6)).format(BASIC_DATE);
		List<StockCandidateHitEntity> rows = list(Wrappers.<StockCandidateHitEntity>lambdaQuery()
				.ge(StockCandidateHitEntity::getTradeDate, cutoff));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("asOf", latest);
		result.put("lookbackSignalDays", window);
		result.put("signalDates", rows.stream().map(StockCandidateHitEntity::getTradeDate).distinct().count());
		result.put("totalCandidates", rows.size());
		result.put("evaluated", rows.stream().filter(r -> r.getEntryPrice() != null).count());
		result.put("skipped", rows.stream().filter(r -> r.getEntryPrice() == null).count());

		// 整体命中（毛收益口径，胜率=fwd_3d>0）
		putOverall(result, "overall", rows);
		// 按模板 / 决策模式（LLM vs 规则 A/B）/ 分数区间
		result.put("byPattern", groupSummary(rows.stream()
				.collect(Collectors.groupingBy(r -> StrUtil.nullToDefault(r.getPattern(), "none")))));
		result.put("byDecisionMode", groupSummary(rows.stream()
				.collect(Collectors.groupingBy(r -> StrUtil.nullToDefault(r.getDecisionMode(), "rules")))));
		result.put("byScoreBucket", groupSummary(rows.stream()
				.collect(Collectors.groupingBy(this::scoreBucket))));
		return result;
	}

	// ==================== 前向收益计算 ====================

	/**
	 * 计算单个候选的命中率行（信号质量独立评估，与模拟持仓无关）
	 */
	private StockCandidateHitEntity computeHit(StockCandidateEntity candidate, List<String> days, int signalIdx) {
		String signalDay = days.get(signalIdx);
		String tsCode = candidate.getTsCode();
		// 信号日至 T+5 的日线（一次查询）
		String endDay = days.get(Math.min(signalIdx + ExitEngine.MAX_HOLD_DAYS, days.size() - 1));
		List<StockDailyEntity> bars = stockDailyMapper.selectList(Wrappers.<StockDailyEntity>lambdaQuery()
				.eq(StockDailyEntity::getTsCode, tsCode)
				.ge(StockDailyEntity::getTradeDate, signalDay)
				.le(StockDailyEntity::getTradeDate, endDay)
				.orderByAsc(StockDailyEntity::getTradeDate));
		Map<String, StockDailyEntity> barByDate = bars.stream()
				.collect(Collectors.toMap(StockDailyEntity::getTradeDate, b -> b, (a, b) -> a));

		StockCandidateHitEntity hit = new StockCandidateHitEntity();
		hit.setTradeDate(signalDay);
		hit.setTsCode(tsCode);
		hit.setName(candidate.getName());
		hit.setPattern(candidate.getPattern());
		hit.setScreenScore(candidate.getScreenScore());
		hit.setLlmScore(candidate.getLlmScore());
		hit.setDecisionMode(candidate.getDecisionMode());
		hit.setConfidence(candidate.getConfidence());

		// 信号日收盘（跳空判定基准）
		StockDailyEntity signalBar = barByDate.get(signalDay);
		if (signalBar == null || signalBar.getClose() == null) {
			return null;
		}
		double signalClose = signalBar.getClose().doubleValue();

		// 入场日 = 信号日次一交易日
		String entryDay = days.get(signalIdx + 1);
		StockDailyEntity entryBar = barByDate.get(entryDay);
		if (entryBar == null || entryBar.getOpen() == null || entryBar.getOpen().doubleValue() <= 0) {
			hit.setEntrySkipped("suspended");
			return hit;
		}
		double entry = entryBar.getOpen().doubleValue();
		if (ExitEngine.entryGapBlocked(signalClose, entry)) {
			hit.setEntrySkipped(entry > signalClose ? "gap_up" : "gap_down");
			return hit;
		}
		hit.setEntryPrice(BigDecimal.valueOf(entry).setScale(3, RoundingMode.HALF_UP));

		// 前向收益（fwd_Nd = T+N 收盘/入场-1，毛收益）
		double best = Double.NEGATIVE_INFINITY;
		for (int n = 1; n <= ExitEngine.MAX_HOLD_DAYS && signalIdx + n < days.size(); n++) {
			StockDailyEntity bar = barByDate.get(days.get(signalIdx + n));
			if (bar == null || bar.getClose() == null) {
				continue;
			}
			double ret = bar.getClose().doubleValue() / entry - 1;
			best = Math.max(best, ret);
			if (n == 1) {
				hit.setFwd1d(scaleRet(ret));
			}
			else if (n == 3) {
				hit.setFwd3d(scaleRet(ret));
			}
			else if (n == 5) {
				hit.setFwd5d(scaleRet(ret));
			}
		}
		if (best != Double.NEGATIVE_INFINITY) {
			hit.setBestRet(scaleRet(best));
		}
		return hit;
	}

	private BigDecimal scaleRet(double v) {
		return BigDecimal.valueOf(Math.round(v * 10000) / 10000.0);
	}

	// ==================== 聚合 ====================

	private void putOverall(Map<String, Object> result, String key, List<StockCandidateHitEntity> rows) {
		Map<String, Object> overall = new LinkedHashMap<>();
		overall.put("n", rows.size());
		putHitMetrics(overall, rows);
		result.put(key, overall);
	}

	private List<Map<String, Object>> groupSummary(Map<String, List<StockCandidateHitEntity>> groups) {
		List<Map<String, Object>> list = new ArrayList<>();
		groups.forEach((key, rows) -> {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("key", key);
			item.put("n", rows.size());
			putHitMetrics(item, rows);
			list.add(item);
		});
		// 按 3 日胜率降序
		list.sort((a, b) -> Double.compare(
				((Number) b.getOrDefault("hitRate3d", 0)).doubleValue(),
				((Number) a.getOrDefault("hitRate3d", 0)).doubleValue()));
		return list;
	}

	private void putHitMetrics(Map<String, Object> item, List<StockCandidateHitEntity> rows) {
		List<BigDecimal> f1 = collect(rows, StockCandidateHitEntity::getFwd1d);
		List<BigDecimal> f3 = collect(rows, StockCandidateHitEntity::getFwd3d);
		List<BigDecimal> f5 = collect(rows, StockCandidateHitEntity::getFwd5d);
		item.put("hitRate1d", hitRate(f1));
		item.put("hitRate3d", hitRate(f3));
		item.put("hitRate5d", hitRate(f5));
		item.put("avgFwd1d", avg(f1));
		item.put("avgFwd3d", avg(f3));
		item.put("avgFwd5d", avg(f5));
	}

	private List<BigDecimal> collect(List<StockCandidateHitEntity> rows,
			java.util.function.Function<StockCandidateHitEntity, BigDecimal> getter) {
		return rows.stream().map(getter).filter(java.util.Objects::nonNull).toList();
	}

	private Object hitRate(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		long wins = values.stream().filter(v -> v.doubleValue() > 0).count();
		return Math.round(wins * 10000.0 / values.size()) / 10000.0;
	}

	private Object avg(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		return Math.round(values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0) * 10000) / 10000.0;
	}

	private String scoreBucket(StockCandidateHitEntity row) {
		double score = row.getScreenScore() != null ? row.getScreenScore().doubleValue() : 0;
		if (score < 70) {
			return "65-70";
		}
		if (score < 75) {
			return "70-75";
		}
		if (score < 80) {
			return "75-80";
		}
		return "80-100";
	}

	// ==================== 通用 ====================

	private int upsertByUniqueKey(String tradeDate, List<StockCandidateHitEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		Map<String, StockCandidateHitEntity> existMap = list(Wrappers.<StockCandidateHitEntity>lambdaQuery()
				.select(StockCandidateHitEntity::getId, StockCandidateHitEntity::getTsCode)
				.eq(StockCandidateHitEntity::getTradeDate, tradeDate))
				.stream()
				.collect(Collectors.toMap(StockCandidateHitEntity::getTsCode, e -> e));
		List<StockCandidateHitEntity> toInsert = new ArrayList<>();
		List<StockCandidateHitEntity> toUpdate = new ArrayList<>();
		for (StockCandidateHitEntity row : rows) {
			StockCandidateHitEntity exist = existMap.get(row.getTsCode());
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
		return rows.size();
	}

	private String latestTradeDate() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		return latest != null ? latest.getTradeDate() : null;
	}

	private List<String> recentTradingDays(String tradeDate, int limit) {
		List<String> days = stockIndexDailyMapper.selectList(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.le(StockIndexDailyEntity::getTradeDate, tradeDate)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit " + limit))
				.stream()
				.map(StockIndexDailyEntity::getTradeDate)
				.collect(Collectors.toList());
		Collections.reverse(days);
		return days;
	}

	private String toIso(String basicDate) {
		return basicDate.length() == 8
				? basicDate.substring(0, 4) + "-" + basicDate.substring(4, 6) + "-" + basicDate.substring(6, 8)
				: basicDate;
	}

}
