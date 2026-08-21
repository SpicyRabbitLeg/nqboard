package com.mx.nqboard.quanta.backtest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockConsWeightEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockConsWeightMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.mapper.StockTopListMapper;
import com.mx.nqboard.quanta.screen.ExitEngine;
import com.mx.nqboard.quanta.screen.ScreenConstants;
import com.mx.nqboard.quanta.screen.ScreenFeatureCalculator;
import com.mx.nqboard.quanta.screen.ScreenFeatures;
import com.mx.nqboard.quanta.screen.ScreenPatternEnum;
import com.mx.nqboard.quanta.screen.ScreenScorer;
import com.mx.nqboard.quanta.screen.UniverseFilter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * <p>
 * 回测引擎（事件驱动模拟，移植 ai-hedge-fund backtest_signals.py）
 * </p>
 * <p>
 * 与实盘筛选共用 ScreenScorer/UniverseFilter/ScreenFeatureCalculator/ExitEngine，
 * 保证回测口径与实盘完全一致（回测可信的前提）。与 Python 版的差异（优化算法）：
 * <pre>
 * 1. 三层筛选（硬门+模板+加分）替代纯加分制，入池线 65 分、每日 TopN=3
 * 2. H7 开盘跳空保护收紧为 +5%/-3%
 * 3. 离场六规则（新增保本止损/弱势提前离场/开盘急杀）
 * 4. 分桶校准：对每个通过入池线的候选（不受仓位约束）独立模拟前向收益，
 *    输出 (分桶, 模板) 维度的胜率/平均收益，用于入池线调优
 * </pre>
 * 已知偏差（与原脚本一致，报告页需明示）：成分股幸存者偏差；资金流/龙虎榜历史
 * 数据缺失时上下文加分自动降级为 0。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BacktestEngine {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String BENCHMARK_INDEX = "sh000300";

	/**
	 * 日线回补缓冲（日历日）：70根交易K线 + 回测区间
	 */
	private static final int FETCH_BUFFER_DAYS = 400;

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

	/**
	 * 执行回测
	 * @param params 回测参数
	 * @param progress 进度回调：(percent, message)
	 * @return 回测报告（成交明细/权益曲线/统计/分桶校准）
	 */
	public BacktestReport run(BacktestParams params, BiConsumer<Integer, String> progress) {
		// ========== 1. 数据加载 ==========
		progress.accept(1, "加载交易日历");
		List<StockIndexDailyEntity> indexBars = stockIndexDailyMapper.selectList(
				Wrappers.<StockIndexDailyEntity>lambdaQuery()
						.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
						.orderByAsc(StockIndexDailyEntity::getTradeDate));
		if (indexBars.size() < 30) {
			throw new IllegalStateException("指数日线数据不足（" + indexBars.size() + " 根），无法回测");
		}
		String endDate = StrUtil.blankToDefault(params.getEndDate(),
				indexBars.get(indexBars.size() - 1).getTradeDate());
		// 截取 endDate 之前的交易日，取最后 days 个
		List<String> allDays = indexBars.stream()
				.map(StockIndexDailyEntity::getTradeDate)
				.filter(d -> d.compareTo(endDate) <= 0)
				.toList();
		if (allDays.size() < params.getDays()) {
			throw new IllegalStateException(String.format("交易日历不足：需要 %d 天，仅有 %d 天（%s~%s）",
					params.getDays(), allDays.size(), allDays.get(0), endDate));
		}
		List<String> days = new ArrayList<>(allDays.subList(allDays.size() - params.getDays(), allDays.size()));
		int nDays = days.size();
		Map<String, Integer> dayIdx = new HashMap<>();
		for (int i = 0; i < nDays; i++) {
			dayIdx.put(days.get(i), i);
		}
		// 指数收盘（含区间前5根，用于大盘环境）
		Map<String, Double> indexClose = new HashMap<>();
		for (StockIndexDailyEntity bar : indexBars) {
			indexClose.put(bar.getTradeDate(), bar.getClose().doubleValue());
		}

		// Universe
		progress.accept(3, "解析股票池");
		Set<String> universeCodes = resolveUniverseCodes(params.getUniverse());

		// 股票基础信息
		List<StockBasicEntity> basics = stockBasicMapper.selectList(Wrappers.emptyWrapper());
		Map<String, StockBasicEntity> basicMap = new HashMap<>();
		for (StockBasicEntity basic : basics) {
			if (universeCodes == null || universeCodes.contains(basic.getTsCode())) {
				basicMap.put(basic.getTsCode(), basic);
			}
		}

		// 日线：区间 + 缓冲（每股票一次查询）
		progress.accept(5, "加载日线数据");
		String fetchStart = LocalDate.parse(toIso(days.get(0))).minusDays(FETCH_BUFFER_DAYS).format(BASIC_DATE);
		Map<String, List<StockDailyEntity>> barsByTs = new LinkedHashMap<>();
		Map<String, Map<String, Integer>> dateIdxByTs = new HashMap<>();
		for (String tsCode : basicMap.keySet()) {
			List<StockDailyEntity> bars = stockDailyMapper.selectList(
					Wrappers.<StockDailyEntity>lambdaQuery()
							.eq(StockDailyEntity::getTsCode, tsCode)
							.ge(StockDailyEntity::getTradeDate, fetchStart)
							.le(StockDailyEntity::getTradeDate, endDate)
							.orderByAsc(StockDailyEntity::getTradeDate));
			if (bars.size() >= ScreenConstants.MIN_BARS) {
				barsByTs.put(tsCode, bars);
				Map<String, Integer> idxMap = new HashMap<>(bars.size() * 2);
				for (int i = 0; i < bars.size(); i++) {
					idxMap.put(bars.get(i).getTradeDate(), i);
				}
				dateIdxByTs.put(tsCode, idxMap);
			}
		}
		log.info("回测数据加载完成: 股票数={}, 交易日={}[{}~{}]", barsByTs.size(), nDays, days.get(0), days.get(nDays - 1));

		// 上下文数据（历史缺失自动降级为无该加分项）
		Map<String, TreeMap<String, Double>> flowByTs = loadFlows(fetchStart, endDate);
		Map<String, String> industryByTs = extractIndustryNames();
		Map<String, Map<String, Double>> sectorPctByDate = loadSectorPct(fetchStart, endDate);
		Map<String, TreeMap<String, Double>> dragonByTs = loadDragons(fetchStart, endDate);

		// ========== 2. 事件驱动模拟 ==========
		double capital = params.getCapital();
		double cash = capital;
		List<Position> openPositions = new ArrayList<>();
		List<Position> pendingSells = new ArrayList<>();
		List<TradeRecord> trades = new ArrayList<>();
		List<Map<String, Object>> equityCurve = new ArrayList<>();
		List<double[]> calibrationSamples = new ArrayList<>(); // [score, fwdRet, patternOrdinal]

		for (int di = 0; di < nDays; di++) {
			String day = days.get(di);

			// --- 2.1 执行挂起卖单（昨收盘触发，今日开盘成交；停牌顺延） ---
			List<Position> stillPending = new ArrayList<>();
			List<Position> sold = new ArrayList<>();
			for (Position pos : pendingSells) {
				StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, pos.tsCode, day);
				if (bar == null || bar.getOpen() == null) {
					stillPending.add(pos);
					continue;
				}
				cash += closePosition(pos, bar.getOpen().doubleValue(), day, di, params, trades);
				sold.add(pos);
			}
			openPositions.removeAll(sold);
			pendingSells = stillPending;

			// --- 2.2 开盘急杀（规则⑥）：当日开盘直接卖，不等收盘 ---
			List<Position> afterGap = new ArrayList<>();
			for (Position pos : openPositions) {
				if (pendingSells.contains(pos)) {
					afterGap.add(pos);
					continue;
				}
				StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, pos.tsCode, day);
				if (bar != null && bar.getOpen() != null && di > pos.entryIdx
						&& ExitEngine.isGapStop(pos.entryPrice, bar.getOpen().doubleValue())) {
					pos.exitReason = "gap_stop";
					cash += closePosition(pos, bar.getOpen().doubleValue(), day, di, params, trades);
				}
				else {
					afterGap.add(pos);
				}
			}
			openPositions = afterGap;

			// --- 2.3 昨日收盘信号 -> 候选 ---
			List<Candidate> candidates = new ArrayList<>();
			if (di >= 1) {
				String signalDay = days.get(di - 1);
				double marketRet5d = marketRet5d(indexClose, allDays, signalDay);
				boolean marketBlocked = marketRet5d < -0.03;
				for (Map.Entry<String, List<StockDailyEntity>> entry : barsByTs.entrySet()) {
					String tsCode = entry.getKey();
					Candidate c = scoreAsOf(entry.getKey(), entry.getValue(), dateIdxByTs.get(tsCode),
							basicMap.get(tsCode), signalDay, marketRet5d, marketBlocked, industryByTs,
							sectorPctByDate, flowByTs, dragonByTs, params);
					if (c != null) {
						candidates.add(c);
						// 分桶校准：所有过线候选独立模拟前向收益（不受仓位约束）
						List<String> window = days.subList(di, Math.min(di + ExitEngine.MAX_HOLD_DAYS + 2, nDays));
						double fwd = simulateForwardByDate(barsByTs, dateIdxByTs, tsCode, signalDay, window, params);
						if (!Double.isNaN(fwd)) {
							calibrationSamples.add(new double[] { c.score, fwd,
									c.pattern != null ? c.pattern.ordinal() : -1 });
						}
					}
				}
				candidates.sort((a, b) -> Double.compare(b.score, a.score));
				if (candidates.size() > params.getTopN()) {
					candidates = new ArrayList<>(candidates.subList(0, params.getTopN()));
				}
			}

			// --- 2.4 今日开盘买入（H7跳空保护 + 仓位约束） ---
			for (Candidate c : candidates) {
				// pendingSells 为 openPositions 子集（停牌顺延单），持仓约束只看总持仓数
				if (openPositions.size() >= params.getMaxPositions()) {
					break;
				}
				if (openPositions.stream().anyMatch(p -> p.tsCode.equals(c.tsCode))
						|| pendingSells.stream().anyMatch(p -> p.tsCode.equals(c.tsCode))) {
					continue;
				}
				StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, c.tsCode, day);
				if (bar == null || bar.getOpen() == null || bar.getOpen().doubleValue() <= 0) {
					continue;
				}
				double openPrice = bar.getOpen().doubleValue();
				if (ExitEngine.entryGapBlocked(c.prevClose, openPrice)) {
					continue;
				}
				double maxBuyValue = capital * params.getMaxPositionPct();
				int qty = (int) Math.floor(maxBuyValue / openPrice / 100) * 100;
				double cost = qty * openPrice * (1 + params.getCostBuy());
				if (qty <= 0) {
					continue;
				}
				if (cost > cash) {
					qty = (int) Math.floor(cash / (openPrice * (1 + params.getCostBuy())) / 100) * 100;
					if (qty <= 0) {
						continue;
					}
					cost = qty * openPrice * (1 + params.getCostBuy());
				}
				cash -= cost;
				Position pos = new Position();
				pos.tsCode = c.tsCode;
				pos.name = c.name;
				pos.pattern = c.pattern;
				pos.qty = qty;
				pos.entryPrice = openPrice;
				pos.entryDate = day;
				pos.entryIdx = di;
				pos.cost = cost;
				pos.peakClose = openPrice;
				pos.signalScore = c.score;
				openPositions.add(pos);
			}

			// --- 2.5 收盘离场评估（规则①-⑤，T+1 次日开盘成交） ---
			for (Position pos : openPositions) {
				if (pendingSells.contains(pos)) {
					continue;
				}
				StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, pos.tsCode, day);
				if (bar == null || bar.getClose() == null) {
					continue;
				}
				double close = bar.getClose().doubleValue();
				pos.peakClose = Math.max(pos.peakClose, close);
				int heldDays = di - pos.entryIdx;
				String reason = ExitEngine.evaluateClose(pos.entryPrice, heldDays, pos.peakClose, close);
				if (reason != null) {
					pos.exitReason = reason;
					pendingSells.add(pos);
				}
			}

			// --- 2.6 逐日盯市 ---
			double equity = cash;
			for (Position pos : openPositions) {
				StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, pos.tsCode, day);
				double price = bar != null && bar.getClose() != null ? bar.getClose().doubleValue() : pos.entryPrice;
				equity += pos.qty * price;
			}
			equityCurve.add(Map.of("date", day, "equity", Math.round(equity * 100) / 100.0));

			if (di % 10 == 0 || di == nDays - 1) {
				progress.accept(5 + (int) ((di + 1) * 90.0 / nDays),
						String.format("模拟进度 %d/%d, 持仓 %d, 成交 %d 笔", di + 1, nDays, openPositions.size(), trades.size()));
			}
		}

		// --- 2.7 强制平仓（期末，仅统计口径） ---
		String lastDay = days.get(nDays - 1);
		for (Position pos : openPositions) {
			StockDailyEntity bar = barOf(barsByTs, dateIdxByTs, pos.tsCode, lastDay);
			double price = bar != null && bar.getClose() != null ? bar.getClose().doubleValue() : pos.entryPrice;
			pos.exitReason = "open_at_end";
			closePosition(pos, price, lastDay, nDays - 1, params, trades);
		}

		// ========== 3. 统计 ==========
		Map<String, Object> stats = buildStats(trades, equityCurve, capital, barsByTs.size(), nDays,
				calibrationSamples);

		BacktestReport report = new BacktestReport();
		report.setTrades(trades);
		report.setEquityCurve(equityCurve);
		report.setStats(stats);
		progress.accept(100, "回测完成");
		return report;
	}

	// ==================== 信号打分（与实盘同一套三层算法） ====================

	/**
	 * 以 signalDay 收盘数据打分，返回通过入池线的候选（未过线返回 null）
	 */
	private Candidate scoreAsOf(String tsCode, List<StockDailyEntity> bars, Map<String, Integer> dateIdx,
			StockBasicEntity basic, String signalDay, double marketRet5d, boolean marketBlocked,
			Map<String, String> industryByTs, Map<String, Map<String, Double>> sectorPctByDate,
			Map<String, TreeMap<String, Double>> flowByTs, Map<String, TreeMap<String, Double>> dragonByTs,
			BacktestParams params) {
		Integer idx = dateIdx != null ? dateIdx.get(signalDay) : null;
		if (idx == null || idx < ScreenConstants.MIN_BARS - 1) {
			return null;
		}
		StockDailyEntity lastBar = bars.get(idx);
		if (!signalDay.equals(lastBar.getTradeDate())) {
			return null;
		}
		// Stage 0 粗滤
		if (universeFilter.stage0RejectReason(basic, lastBar, null) != null) {
			return null;
		}
		// 特征（最近 LOOKBACK_BARS 根窗口）
		int from = Math.max(0, idx + 1 - ScreenConstants.LOOKBACK_BARS);
		List<StockDailyEntity> window = bars.subList(from, idx + 1);
		double limitRatio = ScreenFeatureCalculator.limitRatio(basic.getMarket(), basic.getName());
		ScreenFeatures f = featureCalculator.calculate(window, tsCode, limitRatio);
		if (f == null) {
			return null;
		}
		// 模板匹配先于硬门（H1/H2 按模板差异化豁免，与实盘口径一致）
		ScreenPatternEnum pattern = screenScorer.matchPattern(f);
		// 硬门
		if (!universeFilter.hardGateRejects(f, pattern).isEmpty()) {
			return null;
		}
		// 上下文
		ScreenScorer.ScreenContext ctx = buildContext(tsCode, signalDay, industryByTs, sectorPctByDate,
				flowByTs, dragonByTs, marketRet5d);
		// 打分
		ScreenScorer.ScoreResult result = screenScorer.score(f, ctx, marketBlocked);
		if (result.pattern() == null || !result.passed()) {
			return null;
		}
		Candidate c = new Candidate();
		c.tsCode = tsCode;
		c.name = basic.getName();
		c.pattern = result.pattern();
		c.score = result.totalScore();
		c.prevClose = f.getClose();
		return c;
	}

	/**
	 * 上下文组装（板块/资金流/龙虎榜，历史缺失降级为 null）
	 */
	private ScreenScorer.ScreenContext buildContext(String tsCode, String signalDay,
			Map<String, String> industryByTs, Map<String, Map<String, Double>> sectorPctByDate,
			Map<String, TreeMap<String, Double>> flowByTs, Map<String, TreeMap<String, Double>> dragonByTs,
			double marketRet5d) {
		Double sectorPct = null;
		String industry = industryByTs.get(tsCode);
		if (StrUtil.isNotBlank(industry)) {
			Map<String, Double> pctMap = sectorPctByDate.get(industry);
			if (pctMap != null) {
				sectorPct = pctMap.get(signalDay);
			}
		}
		Double flow3dSum = null;
		Integer flowConsecutive = null;
		TreeMap<String, Double> flows = flowByTs.get(tsCode);
		if (flows != null && !flows.isEmpty()) {
			// signalDay 及之前最近3个交易日
			List<Double> recent = new ArrayList<>();
			Map.Entry<String, Double> floorEntry = flows.floorEntry(signalDay);
			if (floorEntry != null) {
				Map.Entry<String, Double> e = floorEntry;
				while (e != null && recent.size() < 3) {
					recent.add(e.getValue());
					e = flows.lowerEntry(e.getKey());
				}
				flow3dSum = recent.stream().mapToDouble(Double::doubleValue).sum();
				int consecutive = 0;
				for (Double v : recent) {
					if (v > 0) {
						consecutive++;
					}
					else {
						break;
					}
				}
				flowConsecutive = consecutive;
			}
		}
		Double dragonNet = null;
		TreeMap<String, Double> dragons = dragonByTs.get(tsCode);
		if (dragons != null && !dragons.isEmpty()) {
			String windowStart = LocalDate.parse(toIso(signalDay)).minusDays(14).format(BASIC_DATE);
			double sum = 0;
			boolean has = false;
			for (Map.Entry<String, Double> e : dragons.subMap(windowStart, true, signalDay, true).entrySet()) {
				sum += e.getValue();
				has = true;
			}
			if (has) {
				dragonNet = sum;
			}
		}
		return new ScreenScorer.ScreenContext(sectorPct, flow3dSum, flowConsecutive, dragonNet, marketRet5d);
	}

	// ==================== 前向模拟（分桶校准用，不受仓位约束） ====================

	/**
	 * 前向模拟（分桶校准用）：信号日次日开盘虚拟买入，按离场规则模拟至窗口末尾，
	 * 返回扣除成本的净收益率（无法入场/数据不足返回 NaN）。不受仓位约束。
	 * @param dayWindow 信号日之后的交易日窗口（升序）
	 */
	private double simulateForwardByDate(Map<String, List<StockDailyEntity>> barsByTs,
			Map<String, Map<String, Integer>> dateIdxByTs, String tsCode, String signalDay, List<String> dayWindow,
			BacktestParams params) {
		Map<String, Integer> idxMap = dateIdxByTs.get(tsCode);
		List<StockDailyEntity> bars = barsByTs.get(tsCode);
		if (idxMap == null) {
			return Double.NaN;
		}
		Integer signalIdx = idxMap.get(signalDay);
		if (signalIdx == null) {
			return Double.NaN;
		}
		// 找到买入日：signalDay 之后第一个有行情的交易日（dayWindow 内）
		int entryBarIdx = -1;
		for (String day : dayWindow) {
			Integer idx = idxMap.get(day);
			if (idx != null) {
				entryBarIdx = idx;
				break;
			}
		}
		if (entryBarIdx < 0) {
			return Double.NaN;
		}
		double prevClose = bars.get(signalIdx).getClose().doubleValue();
		double entry = bars.get(entryBarIdx).getOpen() != null ? bars.get(entryBarIdx).getOpen().doubleValue() : -1;
		if (entry <= 0 || ExitEngine.entryGapBlocked(prevClose, entry)) {
			return Double.NaN;
		}
		// 模拟离场（规则①-⑤；gap_stop 简化为开盘判断）
		double peak = entry;
		int lastBarIdx = Math.min(entryBarIdx + ExitEngine.MAX_HOLD_DAYS + 1, bars.size() - 1);
		for (int i = entryBarIdx; i <= lastBarIdx; i++) {
			StockDailyEntity bar = bars.get(i);
			if (bar.getClose() == null) {
				continue;
			}
			double close = bar.getClose().doubleValue();
			peak = Math.max(peak, close);
			int heldDays = i - entryBarIdx;
			// 开盘急杀（非买入日）
			if (i > entryBarIdx && bar.getOpen() != null
					&& ExitEngine.isGapStop(entry, bar.getOpen().doubleValue())) {
				return netRet(entry, bar.getOpen().doubleValue(), params);
			}
			String reason = ExitEngine.evaluateClose(entry, heldDays, peak, close);
			if (reason != null) {
				// 次日开盘成交；无次日则按当日收盘
				if (i + 1 <= lastBarIdx && bars.get(i + 1).getOpen() != null) {
					return netRet(entry, bars.get(i + 1).getOpen().doubleValue(), params);
				}
				return netRet(entry, close, params);
			}
		}
		// 未触发离场：按最后一根收盘
		return netRet(entry, bars.get(lastBarIdx).getClose().doubleValue(), params);
	}

	private double netRet(double entry, double exit, BacktestParams params) {
		return exit * (1 - params.getCostSell()) / (entry * (1 + params.getCostBuy())) - 1;
	}

	// ==================== 统计 ====================

	private Map<String, Object> buildStats(List<TradeRecord> trades, List<Map<String, Object>> equityCurve,
			double capital, int universeSize, int nDays, List<double[]> calibrationSamples) {
		List<Double> rets = trades.stream().map(t -> t.ret).toList();
		long wins = rets.stream().filter(r -> r > 0).count();
		long losses = rets.size() - wins;
		double grossWin = rets.stream().filter(r -> r > 0).mapToDouble(Double::doubleValue).sum();
		double grossLoss = Math.abs(rets.stream().filter(r -> r <= 0).mapToDouble(Double::doubleValue).sum());
		double finalEquity = equityCurve.isEmpty() ? capital : (double) equityCurve.get(equityCurve.size() - 1)
				.get("equity");

		double peak = 0;
		double maxDd = 0;
		for (Map<String, Object> point : equityCurve) {
			double eq = (double) point.get("equity");
			peak = Math.max(peak, eq);
			if (peak > 0) {
				maxDd = Math.max(maxDd, (peak - eq) / peak);
			}
		}

		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("universeSize", universeSize);
		stats.put("nDays", nDays);
		stats.put("nTrades", trades.size());
		stats.put("winRate", trades.isEmpty() ? null : round(wins * 1.0 / trades.size()));
		stats.put("avgRet", trades.isEmpty() ? null : round(rets.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
		stats.put("avgWin", wins > 0 ? round(grossWin / wins) : null);
		stats.put("avgLoss", losses > 0 ? round(-grossLoss / losses) : null);
		stats.put("profitFactor", grossLoss > 0 ? round(grossWin / grossLoss) : null);
		stats.put("totalPnl", Math.round((finalEquity - capital) * 100) / 100.0);
		stats.put("totalReturn", round(finalEquity / capital - 1));
		stats.put("maxDrawdown", round(maxDd));
		stats.put("avgHoldingDays", trades.isEmpty() ? null
				: Math.round(trades.stream().mapToInt(t -> t.heldDays).average().orElse(0) * 100) / 100.0);

		// 离场原因分布
		Map<String, Map<String, Object>> exitBreakdown = new LinkedHashMap<>();
		for (TradeRecord t : trades) {
			exitBreakdown.computeIfAbsent(t.reason, k -> new LinkedHashMap<>());
			Map<String, Object> bucket = exitBreakdown.get(t.reason);
			bucket.merge("count", 1, (a, b) -> (int) a + (int) b);
			bucket.merge("totalRet", t.ret, (a, b) -> (double) a + (double) b);
			bucket.merge("pnl", t.pnl, (a, b) -> (double) a + (double) b);
		}
		exitBreakdown.forEach((reason, bucket) -> {
			int count = (int) bucket.get("count");
			bucket.put("avgRet", round((double) bucket.get("totalRet") / count));
			bucket.put("pnl", Math.round((double) bucket.get("pnl") * 100) / 100.0);
			bucket.remove("totalRet");
		});
		stats.put("exitBreakdown", exitBreakdown);

		// 分桶校准：score 桶 x 模板 -> 胜率/平均收益（入池线调优依据）
		stats.put("calibration", buildCalibration(calibrationSamples));
		return stats;
	}

	private Map<String, Object> buildCalibration(List<double[]> samples) {
		// 分桶：[minScore,70) [70,75) [75,80) [80,100]
		String[] bucketNames = { "65-70", "70-75", "75-80", "80-100" };
		Map<String, Map<String, Object>> byBucket = new LinkedHashMap<>();
		for (String name : bucketNames) {
			byBucket.put(name, new LinkedHashMap<>());
		}
		ScreenPatternEnum[] patterns = ScreenPatternEnum.values();
		for (double[] s : samples) {
			double score = s[0];
			double fwdRet = s[1];
			int patternOrd = (int) s[2];
			String bucket = score < 70 ? "65-70" : score < 75 ? "70-75" : score < 80 ? "75-80" : "80-100";
			Map<String, Object> b = byBucket.get(bucket);
			b.merge("n", 1, (a, x) -> (int) a + (int) x);
			b.merge("totalRet", fwdRet, (a, x) -> (double) a + (double) x);
			if (fwdRet > 0) {
				b.merge("wins", 1, (a, x) -> (int) a + (int) x);
			}
			if (patternOrd >= 0 && patternOrd < patterns.length) {
				String key = "p_" + patterns[patternOrd].getCode();
				b.merge(key + "_n", 1, (a, x) -> (int) a + (int) x);
				b.merge(key + "_ret", fwdRet, (a, x) -> (double) a + (double) x);
			}
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("note", "对每个通过入池线的候选独立模拟前向收益（次日开盘入场+离场规则，不受仓位约束）");
		result.put("totalSamples", samples.size());
		byBucket.forEach((bucket, b) -> {
			int n = (int) b.getOrDefault("n", 0);
			if (n == 0) {
				return;
			}
			int wins = (int) b.getOrDefault("wins", 0);
			b.put("winRate", round(wins * 1.0 / n));
			b.put("avgRet", round((double) b.get("totalRet") / n));
			b.remove("totalRet");
			b.remove("wins");
		});
		result.put("buckets", byBucket);
		return result;
	}

	// ==================== 数据加载 ====================

	private Map<String, TreeMap<String, Double>> loadFlows(String fetchStart, String endDate) {
		List<StockMoneyFlowEntity> rows = stockMoneyFlowMapper.selectList(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.ge(StockMoneyFlowEntity::getTradeDate, fetchStart)
						.le(StockMoneyFlowEntity::getTradeDate, endDate)
						.isNotNull(StockMoneyFlowEntity::getMainNetInflow));
		Map<String, TreeMap<String, Double>> map = new HashMap<>();
		for (StockMoneyFlowEntity row : rows) {
			map.computeIfAbsent(row.getTsCode(), k -> new TreeMap<>())
					.put(row.getTradeDate(), row.getMainNetInflow().doubleValue());
		}
		return map;
	}

	/**
	 * 提取行业名映射（ts_code -> 东财行业名），取资金流最新快照日期的行
	 */
	private Map<String, String> extractIndustryNames() {
		List<Object> dates = stockMoneyFlowMapper.selectObjs(Wrappers.<StockMoneyFlowEntity>query()
				.select("MAX(trade_date)"));
		if (CollUtil.isEmpty(dates) || dates.get(0) == null) {
			return new HashMap<>();
		}
		String latestDate = String.valueOf(dates.get(0));
		List<StockMoneyFlowEntity> rows = stockMoneyFlowMapper.selectList(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.eq(StockMoneyFlowEntity::getTradeDate, latestDate)
						.isNotNull(StockMoneyFlowEntity::getIndustryName)
						.select(StockMoneyFlowEntity::getTsCode, StockMoneyFlowEntity::getIndustryName));
		Map<String, String> map = new HashMap<>();
		for (StockMoneyFlowEntity row : rows) {
			map.put(row.getTsCode(), row.getIndustryName());
		}
		return map;
	}

	private Map<String, Map<String, Double>> loadSectorPct(String fetchStart, String endDate) {
		List<StockIndustryDailyEntity> rows = stockIndustryDailyMapper.selectList(
				Wrappers.<StockIndustryDailyEntity>lambdaQuery()
						.ge(StockIndustryDailyEntity::getTradeDate, fetchStart)
						.le(StockIndustryDailyEntity::getTradeDate, endDate)
						.isNotNull(StockIndustryDailyEntity::getClose)
						.isNotNull(StockIndustryDailyEntity::getBoardName)
						.orderByAsc(StockIndustryDailyEntity::getTradeDate));
		// board -> date -> close
		Map<String, TreeMap<String, Double>> closeByBoard = new HashMap<>();
		for (StockIndustryDailyEntity row : rows) {
			closeByBoard.computeIfAbsent(row.getBoardName(), k -> new TreeMap<>())
					.put(row.getTradeDate(), row.getClose().doubleValue());
		}
		Map<String, Map<String, Double>> pctByBoard = new HashMap<>();
		closeByBoard.forEach((board, tree) -> {
			Map<String, Double> pctMap = new HashMap<>();
			String prevDate = null;
			double prevClose = 0;
			for (Map.Entry<String, Double> e : tree.entrySet()) {
				if (prevDate != null && prevClose > 0) {
					pctMap.put(e.getKey(), e.getValue() / prevClose - 1);
				}
				prevDate = e.getKey();
				prevClose = e.getValue();
			}
			pctByBoard.put(board, pctMap);
		});
		return pctByBoard;
	}

	private Map<String, TreeMap<String, Double>> loadDragons(String fetchStart, String endDate) {
		List<StockTopListEntity> rows = stockTopListMapper.selectList(
				Wrappers.<StockTopListEntity>lambdaQuery()
						.ge(StockTopListEntity::getTradeDate, fetchStart)
						.le(StockTopListEntity::getTradeDate, endDate)
						.isNotNull(StockTopListEntity::getNetAmount));
		Map<String, TreeMap<String, Double>> map = new HashMap<>();
		for (StockTopListEntity row : rows) {
			map.computeIfAbsent(row.getTsCode(), k -> new TreeMap<>())
					.merge(row.getTradeDate(), row.getNetAmount().doubleValue(), Double::sum);
		}
		return map;
	}

	private Set<String> resolveUniverseCodes(String universe) {
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
				continue;
			}
			stockConsWeightMapper.selectList(Wrappers.<StockConsWeightEntity>lambdaQuery()
					.eq(StockConsWeightEntity::getIndexCode, indexCode)
					.eq(StockConsWeightEntity::getTradeDate, latest.getTradeDate())
					.select(StockConsWeightEntity::getTsCode))
					.forEach(r -> codes.add(r.getTsCode()));
		}
		return codes;
	}

	private double marketRet5d(Map<String, Double> indexClose, List<String> allDays, String asOf) {
		int idx = -1;
		for (int i = allDays.size() - 1; i >= 0; i--) {
			if (allDays.get(i).equals(asOf)) {
				idx = i;
				break;
			}
		}
		if (idx < 5) {
			return 0;
		}
		Double last = indexClose.get(allDays.get(idx));
		Double first = indexClose.get(allDays.get(idx - 5));
		if (last == null || first == null || first <= 0) {
			return 0;
		}
		return last / first - 1;
	}

	// ==================== 内部结构 ====================

	private StockDailyEntity barOf(Map<String, List<StockDailyEntity>> barsByTs,
			Map<String, Map<String, Integer>> dateIdxByTs, String tsCode, String day) {
		Map<String, Integer> idxMap = dateIdxByTs.get(tsCode);
		if (idxMap == null) {
			return null;
		}
		Integer idx = idxMap.get(day);
		return idx != null ? barsByTs.get(tsCode).get(idx) : null;
	}

	/**
	 * 平仓成交并记录 trade（返回卖出净得现金）
	 */
	private double closePosition(Position pos, double exitPrice, String exitDay, int exitIdx,
			BacktestParams params, List<TradeRecord> trades) {
		double proceeds = pos.qty * exitPrice * (1 - params.getCostSell());
		TradeRecord t = new TradeRecord();
		t.tsCode = pos.tsCode;
		t.name = pos.name;
		t.pattern = pos.pattern != null ? pos.pattern.getCode() : null;
		t.entryDate = pos.entryDate;
		t.entryPrice = pos.entryPrice;
		t.exitDate = exitDay;
		t.exitPrice = exitPrice;
		t.qty = pos.qty;
		t.reason = pos.exitReason;
		t.pnl = proceeds - pos.cost;
		t.ret = proceeds / pos.cost - 1;
		t.heldDays = exitIdx - pos.entryIdx;
		t.signalScore = pos.signalScore;
		trades.add(t);
		return proceeds;
	}

	@Data
	static class Position {

		String tsCode;

		String name;

		ScreenPatternEnum pattern;

		int qty;

		double entryPrice;

		String entryDate;

		int entryIdx;

		double cost;

		double peakClose;

		double signalScore;

		String exitReason;

	}

	@Data
	static class Candidate {

		String tsCode;

		String name;

		ScreenPatternEnum pattern;

		double score;

		double prevClose;

	}

	@Data
	public static class TradeRecord {

		String tsCode;

		String name;

		String pattern;

		String entryDate;

		double entryPrice;

		String exitDate;

		double exitPrice;

		int qty;

		String reason;

		double pnl;

		double ret;

		int heldDays;

		double signalScore;

	}

	@Data
	public static class BacktestReport {

		List<TradeRecord> trades;

		List<Map<String, Object>> equityCurve;

		Map<String, Object> stats;

	}

	private double round(double v) {
		return Math.round(v * 10000) / 10000.0;
	}

	private String toIso(String basicDate) {
		return basicDate.length() == 8
				? basicDate.substring(0, 4) + "-" + basicDate.substring(4, 6) + "-" + basicDate.substring(6, 8)
				: basicDate;
	}

}
