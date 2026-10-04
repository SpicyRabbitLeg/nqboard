package com.mx.nqboard.sniper.data.ingest;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.service.AdjFactorService;
import com.mx.nqboard.sniper.service.DailyRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 每日数据更新任务编排（主文档 §4.4 五阶段；Quartz Job 与手动触发
 * 共用本服务，经 {@code DailyRunService.runGuarded} 幂等包装）：
 * </p>
 *
 * <pre>
 * ① 15:05 快照        东财 clist 3 级链 → sniper_market_snapshot（预算无条件计 1，耗尽返回空）
 * ② 15:08 交易日历    trade_cal 增量窗口
 * ③ 15:10 行情增量    daily/adj_factor 全市场各 1 调 → none 行+因子（含当日 qfq 合成）
 * ④ 15:12 除权检测    当日 factor ≠ 前一交易日 → 变化票全历史 qfq 库内重算覆盖
 * ⑤ 15:15 事件数据    龙虎榜 top_list 全市场 1 调（其余事件表 deep 票预检按需"拉取即入库"）
 *    + 指数行情        000300/000905/399006 增量 30 日（市场门与台账基准消费）
 * </pre>
 *
 * <p>阶段失败<b>记缺数标记继续</b>（不终止序列）；仅行情③失败时跳过④（qfq 依赖当日 none+因子）。
 * 除权检测防护：前一日因子为空（回填未做）时跳过重算，避免全市场误重算。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyUpdateService {

	private static final List<String> INDEX_CODES = List.of("000300", "000905", "399006");

	private final DataIngestService dataIngestService;

	private final EventIngestService eventIngestService;

	private final AdjFactorService adjFactorService;

	private final DailyRunService dailyRunService;

	private final DailyBudget dailyBudget;

	private final com.mx.nqboard.sniper.data.DataGateway dataGateway;

	/** 五阶段全序列（Quartz data_update Job 与手动触发同一路径；幂等可重跑）。
	 *  runDate 为非交易日时自动回退最近交易日（照 Python resolve_as_of_date 语义），
	 *  记账与快照/行情数据日期均锚定回退后的 asOf。 */
	public Map<String, Object> runDataUpdate(LocalDate runDate) {
		LocalDate asOf = dataGateway.resolveAsOf(runDate);
		if (!asOf.equals(runDate)) {
			log.info("runDataUpdate: {} is non-trading day, resolved to {}", runDate, asOf);
		}
		return dailyRunService.runGuarded(asOf, RunPhaseEnum.DATA_UPDATE, () -> doRunDataUpdate(asOf));
	}

	private Map<String, Object> doRunDataUpdate(LocalDate runDate) {
		Map<String, Object> detail = new LinkedHashMap<>();

		// ① 快照（15:05）
		try {
			detail.put("spotRows", dataIngestService.ingestSpot(runDate, null));
		}
		catch (RuntimeException e) {
			detail.put("spotFailed", mark(e));
		}

		// ② 交易日历（15:08）：增量窗口（预灌全量后每日刷尾部）
		try {
			detail.put("calendarRows", dataIngestService.ingestTradeCalendar(runDate.minusDays(7).toString(),
					runDate.plusDays(400).toString()));
		}
		catch (RuntimeException e) {
			detail.put("calendarFailed", mark(e));
		}

		// ③④ 行情增量 + 当日 qfq 合成（15:10/15:12；qfq 依赖当日 none+因子，失败则跳过除权检测）
		try {
			detail.putAll(dataIngestService.ingestDailyAndFactors(runDate));
			try {
				detail.put("exDividendRecalcs", detectAndRecalcExDividend(runDate));
			}
			catch (RuntimeException e) {
				detail.put("exDividendFailed", mark(e));
			}
		}
		catch (RuntimeException e) {
			detail.put("dailyFailed", mark(e));
		}

		// ⑤ 事件数据（15:15）：龙虎榜全市场 1 调；其余事件表 deep 票预检按需拉取
		try {
			detail.put("dragonRows", eventIngestService.ingestDragonTiger(runDate));
		}
		catch (RuntimeException e) {
			detail.put("dragonFailed", mark(e));
		}

		// 指数行情（市场门 5 日涨幅 / 台账沪深300 基准消费）
		try {
			Map<String, Integer> indexRows = new LinkedHashMap<>();
			for (String index : INDEX_CODES) {
				indexRows.put(index, dataIngestService.ingestIndexDaily(index, runDate.minusDays(30), runDate));
			}
			detail.put("indexRows", indexRows);
		}
		catch (RuntimeException e) {
			detail.put("indexFailed", mark(e));
		}

		detail.put("budgetUsed", dailyBudget.used());
		return detail;
	}

	/**
	 * 除权检测（§4.3.4）：当日 factor ≠ 该票前一交易日 factor → 该票全历史 qfq 库内重算覆盖。
	 * @return 重算票数
	 */
	public int detectAndRecalcExDividend(LocalDate tradeDate) {
		Map<String, java.math.BigDecimal> today = factorMap(tradeDate);
		Map<String, java.math.BigDecimal> prev = factorMap(latestTradingDayBefore(tradeDate));
		if (today.isEmpty() || prev.isEmpty()) {
			// 回填未做（前一日因子为空）：跳过，避免全市场误重算
			log.info("ex-dividend detection skipped: today={} prev={}", today.size(), prev.size());
			return 0;
		}
		int recalcs = 0;
		for (Map.Entry<String, java.math.BigDecimal> entry : today.entrySet()) {
			java.math.BigDecimal prevFactor = prev.get(entry.getKey());
			if (prevFactor == null || prevFactor.compareTo(entry.getValue()) != 0) {
				recalcs += dataIngestService.recalcQfqFull(entry.getKey());
			}
		}
		log.info("ex-dividend detection {}: {} recalcs", tradeDate, recalcs);
		return recalcs;
	}

	private Map<String, java.math.BigDecimal> factorMap(LocalDate date) {
		if (date == null) {
			return Map.of();
		}
		Map<String, java.math.BigDecimal> map = new LinkedHashMap<>();
		for (AdjFactorEntity e : adjFactorService.list(Wrappers.<AdjFactorEntity>lambdaQuery()
			.eq(AdjFactorEntity::getTradeDate, date))) {
			map.put(e.getCode(), e.getFactor());
		}
		return map;
	}

	private LocalDate latestTradingDayBefore(LocalDate date) {
		if (date == null) {
			return null;
		}
		// 直接查因子表的最近前置日期（不依赖日历：因子表即事实交易日集合）
		AdjFactorEntity latest = adjFactorService.getOne(Wrappers.<AdjFactorEntity>lambdaQuery()
			.lt(AdjFactorEntity::getTradeDate, date)
			.orderByDesc(AdjFactorEntity::getTradeDate)
			.last("LIMIT 1"), false);
		return latest == null ? null : latest.getTradeDate();
	}

	private static String mark(RuntimeException e) {
		return e.getMessage() == null ? e.toString() : e.getMessage();
	}

}
