package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockConsWeightEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockConsWeightMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.mapper.StockScreenResultMapper;
import com.mx.nqboard.quanta.mapper.StockTopListMapper;
import com.mx.nqboard.quanta.screen.MarketRegimeCalculator;
import com.mx.nqboard.quanta.screen.ScreenConstants;
import com.mx.nqboard.quanta.screen.ScreenFeatureCalculator;
import com.mx.nqboard.quanta.screen.ScreenFeatures;
import com.mx.nqboard.quanta.screen.ScreenScorer;
import com.mx.nqboard.quanta.screen.UniverseFilter;
import com.mx.nqboard.quanta.service.StockScreenResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 每日筛选打分结果 服务实现类（筛选引擎编排）
 * </p>
 * <p>
 * 三层算法：Stage 0 粗滤（流动性/ST/板块/次新/涨停）-> Stage 0.5 硬门（H1-H6）
 * -> 模板匹配（四选一）+ 质量加分（板块共振/主力资金/龙虎榜/RSI/大盘/反追高/连板）。
 * 上下文数据（资金流/板块/龙虎榜）批量预加载后按 ts_code 关联，单股票仅一次日线查询。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockScreenResultServiceImpl extends ServiceImpl<StockScreenResultMapper, StockScreenResultEntity>
		implements StockScreenResultService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 大盘基准指数
	 */
	private static final String BENCHMARK_INDEX = "sh000300";

	/**
	 * 龙虎榜回看天数（日历日，覆盖5个交易日）
	 */
	private static final int DRAGON_LOOKBACK_DAYS = 14;

	private final StockScreenResultMapper stockScreenResultMapper;

	private final StockBasicMapper stockBasicMapper;

	private final StockDailyMapper stockDailyMapper;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	private final StockConsWeightMapper stockConsWeightMapper;

	private final StockMoneyFlowMapper stockMoneyFlowMapper;

	private final StockIndustryDailyMapper stockIndustryDailyMapper;

	private final StockTopListMapper stockTopListMapper;

	private final ScreenFeatureCalculator featureCalculator;

	private final UniverseFilter universeFilter;

	private final ScreenScorer screenScorer;

	private final MarketRegimeCalculator marketRegimeCalculator;

	/**
	 * 股票池范围（yml quanta.screen.universe）：all/hs300_csi500/hs300/csi500
	 */
	@Value("${quanta.screen.universe:hs300_csi500}")
	private String universe;

	/**
	 * 数据就绪检查：股票日线当日最低覆盖率
	 */
	@Value("${quanta.pipeline.readiness-min-coverage:0.95}")
	private double readinessMinCoverage;

	@Override
	public int screen() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		if (latest == null || StrUtil.isBlank(latest.getTradeDate())) {
			throw new IllegalStateException("指数日线无数据，请先执行 index_daily 同步步骤");
		}
		return screen(latest.getTradeDate());
	}

	@Override
	public int screen(String tradeDate) {
		tradeDate = toBasicDate(tradeDate);
		// 1. 数据就绪检查
		checkReadiness(tradeDate);

		// 2. 大盘环境（H8 大盘门）
		MarketRegimeCalculator.MarketRegime regime = marketRegimeCalculator.calc(tradeDate);
		log.info("筛选打分开始, 信号日={}, 大盘5日收益={}, 大盘门={}", tradeDate, regime.ret5d(), regime.blocked());

		// 3. Universe 解析
		Set<String> universeCodes = resolveUniverseCodes();

		// 4. 股票基础信息
		List<StockBasicEntity> basics = stockBasicMapper.selectList(Wrappers.emptyWrapper());
		Map<String, StockBasicEntity> basicMap = basics.stream()
				.collect(Collectors.toMap(StockBasicEntity::getTsCode, b -> b, (a, b) -> a));

		// 5. 上下文数据批量预加载
		Map<String, List<StockMoneyFlowEntity>> flowByTs = loadMoneyFlow(tradeDate);
		Map<String, String> industryNameByTs = extractIndustryNames(flowByTs, tradeDate);
		Map<String, Double> sectorPctByBoard = loadSectorPct(tradeDate);
		Map<String, Double> dragonNetByTs = loadDragonNet(tradeDate);

		// 6. 逐股票筛选打分
		List<StockScreenResultEntity> results = new ArrayList<>();
		int stage0Rejected = 0;
		int insufficientBars = 0;
		for (StockBasicEntity basic : basics) {
			String tsCode = basic.getTsCode();
			if (universeCodes != null && !universeCodes.contains(tsCode)) {
				continue;
			}
			// 日线（最新 lookback 根，升序）
			List<StockDailyEntity> bars = loadBars(tsCode, tradeDate);
			if (CollUtil.isEmpty(bars)) {
				continue;
			}
			StockDailyEntity lastBar = bars.get(bars.size() - 1);
			// 信号日无行情（停牌）跳过
			if (!tradeDate.equals(lastBar.getTradeDate())) {
				continue;
			}
			// Stage 0 粗滤
			String stage0Reason = universeFilter.stage0RejectReason(basic, lastBar, null);
			if (stage0Reason != null) {
				stage0Rejected++;
				continue;
			}
			// 特征计算（数据不足跳过，多为次新）
			ScreenFeatures features = featureCalculator.calculate(ScreenFeatureCalculator.reverse(bars), tsCode,
					ScreenFeatureCalculator.limitRatio(basic.getMarket(), basic.getName()));
			if (features == null) {
				insufficientBars++;
				continue;
			}
			// Stage 0.5 硬门
			List<String> hardRejects = universeFilter.hardGateRejects(features);
			// 上下文
			ScreenScorer.ScreenContext ctx = buildContext(tsCode, features, industryNameByTs, sectorPctByBoard,
					flowByTs, dragonNetByTs, regime);
			// 打分
			ScreenScorer.ScoreResult score = screenScorer.score(features, ctx, regime.blocked());

			StockScreenResultEntity entity = new StockScreenResultEntity();
			entity.setTradeDate(tradeDate);
			entity.setTsCode(tsCode);
			entity.setName(basic.getName());
			entity.setMetrics(JSON.toJSONString(score.metrics()));
			if (!hardRejects.isEmpty()) {
				entity.setPassed("0");
				entity.setRejectReason(String.join(";", hardRejects));
				results.add(entity);
				continue;
			}
			if (score.pattern() == null) {
				entity.setPassed("0");
				entity.setPattern("none");
				entity.setRejectReason("未命中入场模板");
				results.add(entity);
				continue;
			}
			entity.setPattern(score.pattern().getCode());
			entity.setPatternScore(BigDecimal.valueOf(score.patternScore()));
			entity.setScreenScore(BigDecimal.valueOf(score.totalScore()));
			if (score.passed()) {
				entity.setPassed("1");
				entity.setRejectReason(null);
			}
			else {
				entity.setPassed("0");
				entity.setRejectReason(score.totalScore() < minScore ? "低于入池线" : null);
			}
			results.add(entity);
		}
		log.info("筛选打分统计, 信号日={}, 基础池={}, Stage0剔除={}, 数据不足={}, 落库={}",
				tradeDate, basics.size(), stage0Rejected, insufficientBars, results.size());

		// 7. upsert 落库（按 trade_date + ts_code）
		int affected = upsertByUniqueKey(tradeDate, results);

		long passedCount = results.stream().filter(r -> "1".equals(r.getPassed())).count();
		log.info("筛选打分完成, 信号日={}, 通过入池线 {} 只", tradeDate, passedCount);
		return affected;
	}

	@Override
	public List<StockScreenResultEntity> topCandidates(String tradeDate, int topN) {
		tradeDate = toBasicDate(tradeDate);
		return list(Wrappers.<StockScreenResultEntity>lambdaQuery()
				.eq(StockScreenResultEntity::getTradeDate, tradeDate)
				.eq(StockScreenResultEntity::getPassed, "1")
				.orderByDesc(StockScreenResultEntity::getScreenScore)
				.last("limit " + Math.max(1, topN)));
	}

	/**
	 * 上下文组装（板块共振/主力资金/龙虎榜）
	 */
	private ScreenScorer.ScreenContext buildContext(String tsCode, ScreenFeatures features,
			Map<String, String> industryNameByTs, Map<String, Double> sectorPctByBoard,
			Map<String, List<StockMoneyFlowEntity>> flowByTs, Map<String, Double> dragonNetByTs,
			MarketRegimeCalculator.MarketRegime regime) {
		// 板块共振：资金流快照携带的东财行业名 -> 板块当日涨幅
		Double sectorPct = null;
		String industryName = industryNameByTs.get(tsCode);
		if (StrUtil.isNotBlank(industryName)) {
			sectorPct = sectorPctByBoard.get(industryName);
		}
		// 主力资金：3日累计净流入 + 连续净流入天数
		Double flow3dSum = null;
		Integer flowConsecutive = null;
		List<StockMoneyFlowEntity> flows = flowByTs.get(tsCode);
		if (CollUtil.isNotEmpty(flows)) {
			double sum = 0;
			for (StockMoneyFlowEntity flow : flows) {
				if (flow.getMainNetInflow() != null) {
					sum += flow.getMainNetInflow().doubleValue();
				}
			}
			flow3dSum = sum;
			int consecutive = 0;
			for (StockMoneyFlowEntity flow : flows) {
				if (flow.getMainNetInflow() != null && flow.getMainNetInflow().doubleValue() > 0) {
					consecutive++;
				}
				else {
					break;
				}
			}
			flowConsecutive = consecutive;
		}
		// 龙虎榜：窗口内净买额合计
		Double dragonNet = dragonNetByTs.get(tsCode);
		return new ScreenScorer.ScreenContext(sectorPct, flow3dSum, flowConsecutive, dragonNet, regime.ret5d());
	}

	/**
	 * 加载近3个交易日的资金流（按 ts_code 分组，日期降序）
	 */
	private Map<String, List<StockMoneyFlowEntity>> loadMoneyFlow(String tradeDate) {
		List<Object> dates = stockMoneyFlowMapper.selectObjs(Wrappers.<StockMoneyFlowEntity>query()
				.select("DISTINCT trade_date")
				.le("trade_date", tradeDate)
				.last("order by trade_date desc limit 3"));
		if (CollUtil.isEmpty(dates)) {
			return new HashMap<>();
		}
		List<String> dateList = dates.stream().map(String::valueOf).toList();
		List<StockMoneyFlowEntity> rows = stockMoneyFlowMapper.selectList(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery().in(StockMoneyFlowEntity::getTradeDate, dateList));
		// 日期降序（最新在前），供连续流入天数计算
		return rows.stream()
				.sorted((a, b) -> b.getTradeDate().compareTo(a.getTradeDate()))
				.collect(Collectors.groupingBy(StockMoneyFlowEntity::getTsCode));
	}

	/**
	 * 提取信号日各股票的东财行业名（来自资金流快照 f100）
	 */
	private Map<String, String> extractIndustryNames(Map<String, List<StockMoneyFlowEntity>> flowByTs,
			String tradeDate) {
		Map<String, String> industryByTs = new HashMap<>();
		flowByTs.forEach((tsCode, flows) -> flows.stream()
				.filter(f -> tradeDate.equals(f.getTradeDate()) && StrUtil.isNotBlank(f.getIndustryName()))
				.findFirst()
				.ifPresent(f -> industryByTs.put(tsCode, f.getIndustryName())));
		return industryByTs;
	}

	/**
	 * 加载行业板块当日涨幅（近2个交易日收盘价计算）
	 */
	private Map<String, Double> loadSectorPct(String tradeDate) {
		List<Object> dates = stockIndustryDailyMapper.selectObjs(Wrappers.<StockIndustryDailyEntity>query()
				.select("DISTINCT trade_date")
				.le("trade_date", tradeDate)
				.last("order by trade_date desc limit 2"));
		if (dates.size() < 2) {
			return new HashMap<>();
		}
		String latest = String.valueOf(dates.get(0));
		String prev = String.valueOf(dates.get(1));
		Map<String, Double> latestClose = boardCloseMap(latest);
		Map<String, Double> prevClose = boardCloseMap(prev);
		Map<String, Double> pctMap = new HashMap<>();
		latestClose.forEach((boardName, close) -> {
			Double p = prevClose.get(boardName);
			if (p != null && p > 0) {
				pctMap.put(boardName, close / p - 1);
			}
		});
		return pctMap;
	}

	private Map<String, Double> boardCloseMap(String tradeDate) {
		return stockIndustryDailyMapper.selectList(Wrappers.<StockIndustryDailyEntity>lambdaQuery()
				.eq(StockIndustryDailyEntity::getTradeDate, tradeDate)
				.isNotNull(StockIndustryDailyEntity::getBoardName))
				.stream()
				.filter(b -> b.getClose() != null && b.getClose().doubleValue() > 0)
				.collect(Collectors.toMap(StockIndustryDailyEntity::getBoardName,
						b -> b.getClose().doubleValue(), (a, b) -> a));
	}

	/**
	 * 加载龙虎榜窗口内净买额合计（按 ts_code）
	 */
	private Map<String, Double> loadDragonNet(String tradeDate) {
		String startDate = LocalDate.parse(toIsoDate(tradeDate)).minusDays(DRAGON_LOOKBACK_DAYS).format(BASIC_DATE);
		List<StockTopListEntity> rows = stockTopListMapper.selectList(Wrappers.<StockTopListEntity>lambdaQuery()
				.ge(StockTopListEntity::getTradeDate, startDate)
				.le(StockTopListEntity::getTradeDate, tradeDate)
				.isNotNull(StockTopListEntity::getNetAmount));
		Map<String, Double> netByTs = new HashMap<>();
		for (StockTopListEntity row : rows) {
			netByTs.merge(row.getTsCode(), row.getNetAmount().doubleValue(), Double::sum);
		}
		return netByTs;
	}

	/**
	 * 加载单只股票信号日前 lookback 根日线（倒序查询后返回升序）
	 */
	private List<StockDailyEntity> loadBars(String tsCode, String tradeDate) {
		List<StockDailyEntity> bars = stockDailyMapper.selectList(Wrappers.<StockDailyEntity>lambdaQuery()
				.eq(StockDailyEntity::getTsCode, tsCode)
				.le(StockDailyEntity::getTradeDate, tradeDate)
				.orderByDesc(StockDailyEntity::getTradeDate)
				.last("limit " + ScreenConstants.LOOKBACK_BARS));
		return bars;
	}

	/**
	 * Universe 解析：hs300/csi500 取指数成分股最新权重表，all 返回 null（不过滤）
	 */
	private Set<String> resolveUniverseCodes() {
		if (StrUtil.isBlank(universe) || "all".equalsIgnoreCase(universe)) {
			return null;
		}
		Set<String> indexCodes = new HashSet<>();
		if (universe.contains("hs300")) {
			indexCodes.add("000300");
		}
		if (universe.contains("csi500")) {
			indexCodes.add("000905");
		}
		if (indexCodes.isEmpty()) {
			return null;
		}
		Set<String> codes = new HashSet<>();
		for (String indexCode : indexCodes) {
			StockConsWeightEntity latest = stockConsWeightMapper.selectOne(
					Wrappers.<StockConsWeightEntity>lambdaQuery()
							.eq(StockConsWeightEntity::getIndexCode, indexCode)
							.orderByDesc(StockConsWeightEntity::getTradeDate)
							.last("limit 1"));
			if (latest == null) {
				log.warn("指数 {} 无成分股权重数据（stock_cons_weight），该指数过滤跳过", indexCode);
				continue;
			}
			List<StockConsWeightEntity> rows = stockConsWeightMapper.selectList(
					Wrappers.<StockConsWeightEntity>lambdaQuery()
							.eq(StockConsWeightEntity::getIndexCode, indexCode)
							.eq(StockConsWeightEntity::getTradeDate, latest.getTradeDate())
							.select(StockConsWeightEntity::getTsCode));
			rows.forEach(r -> codes.add(r.getTsCode()));
		}
		log.info("Universe={}, 成分股数={}", universe, codes.size());
		return codes;
	}

	/**
	 * 数据就绪检查：股票日线当日覆盖率
	 */
	private void checkReadiness(String tradeDate) {
		tradeDate = toBasicDate(tradeDate);
		long basicCount = stockBasicMapper.selectCount(Wrappers.emptyWrapper());
		long dailyCount = stockDailyMapper.selectCount(Wrappers.<StockDailyEntity>lambdaQuery()
				.eq(StockDailyEntity::getTradeDate, tradeDate));
		double coverage = basicCount > 0 ? (double) dailyCount / basicCount : 0;
		if (basicCount > 0 && coverage < readinessMinCoverage) {
			throw new IllegalStateException(String.format(
					"数据未就绪：信号日 %s 股票日线覆盖率 %.1f%%（%d/%d），低于阈值 %.0f%%，请先完成 stock_daily 同步",
					tradeDate, coverage * 100, dailyCount, basicCount, readinessMinCoverage * 100));
		}
	}

	/**
	 * 按 (trade_date, ts_code) upsert：已存在走 update（保留原 id），其余批量插入
	 */
	private int upsertByUniqueKey(String tradeDate, List<StockScreenResultEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		Map<String, StockScreenResultEntity> existMap = list(Wrappers.<StockScreenResultEntity>lambdaQuery()
				.select(StockScreenResultEntity::getId, StockScreenResultEntity::getTsCode)
				.eq(StockScreenResultEntity::getTradeDate, tradeDate))
				.stream()
				.collect(Collectors.toMap(StockScreenResultEntity::getTsCode, e -> e));
		List<StockScreenResultEntity> toInsert = new ArrayList<>();
		List<StockScreenResultEntity> toUpdate = new ArrayList<>();
		for (StockScreenResultEntity row : rows) {
			StockScreenResultEntity exist = existMap.get(row.getTsCode());
			if (exist == null) {
				toInsert.add(row);
			}
			else {
				row.setId(exist.getId());
				toUpdate.add(row);
			}
		}
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert, 500);
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, 500);
		}
		return rows.size();
	}

	/**
	 * 入池线（与 ScreenScorer 一致，仅用于落库原因描述）
	 */
	@Value("${quanta.screen.min-score:" + ScreenConstants.MIN_SCORE + "}")
	private double minScore;

	private String toIsoDate(String basicDate) {
		return basicDate.length() == 8
				? basicDate.substring(0, 4) + "-" + basicDate.substring(4, 6) + "-" + basicDate.substring(6, 8)
				: basicDate;
	}

	private String toBasicDate(String date) {
		if (StrUtil.isBlank(date)) {
			return date;
		}
		String d = date.trim();
		return d.length() == 10 && d.charAt(4) == '-' ? d.replace("-", "") : d;
	}


}
