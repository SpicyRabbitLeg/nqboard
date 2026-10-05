package com.mx.nqboard.sniper.data.ingest;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.data.DataGateway;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.service.DailyRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 历史回填（一次性长任务，主文档 §4.5）：全库基础数据 → 逐交易日 none 行+因子（全市场各 1 调，
 * ~250 调/年）→ 全市场 qfq 一次性合成（逐票 {@code recalcQfqFull}：库内 none+因子重算，
 * max_factor=全历史——因子单调不减，最新因子即全历史 max，窗口回填与全历史等价，T15 抽样实证）→
 * 抽样导出供 Python pro_bar qfq 比对（bit-exact 验证门：按存储精度取整后相等）。
 * </p>
 * <p>运行形态：单线程 executor 串行防重叠 + running 标志；进度经 {@code DailyRunService.runGuarded}
 * 记账（run_date=触发日、phase=data_update，任务运行记录页可见）。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillService {

	private final DataIngestService dataIngestService;

	private final DailyRunService dailyRunService;

	private final DataGateway dataGateway;

	private final DailyBudget dailyBudget;

	private final com.mx.nqboard.sniper.data.provider.em.EastmoneyClient eastmoneyClient;

	private final com.mx.nqboard.sniper.service.StockBasicService stockBasicService;

	private final AtomicBoolean backfilling = new AtomicBoolean(false);

	private final AtomicBoolean patching = new AtomicBoolean(false);

	/** 提交异步回填（单线程串行；已在跑时拒绝重复提交） */
	public boolean submit(LocalDate startDate, LocalDate endDate) {
		if (!backfilling.compareAndSet(false, true)) {
			log.warn("backfill already running; reject submit {}~{}", startDate, endDate);
			return false;
		}
		CompletableFuture.runAsync(() -> {
			try {
				runBackfill(startDate, endDate);
			}
			finally {
				backfilling.set(false);
			}
		});
		return true;
	}

	/** 提交异步 f127 行业修正跑批（已在跑时拒绝重复提交） */
	public boolean submitIndustryPatch() {
		if (!patching.compareAndSet(false, true)) {
			log.warn("industry patch already running; reject submit");
			return false;
		}
		CompletableFuture.runAsync(() -> {
			try {
				dailyRunService.runGuarded(LocalDate.now(), RunPhaseEnum.INDUSTRY_PATCH, this::patchEmIndustry);
			}
			finally {
				patching.set(false);
			}
		});
		return true;
	}

	/**
	 * 东财 f127 行业修正跑批（§3.2 现用口径，附录 A #20⑤）：stock_basic 全量逐票、EM 节流约
	 * 0.5s/票（EastmoneyClient 内建 500ms 下限）、f127 非空才覆盖 industry/industry_source='em'、
	 * 单票失败保持 tushare 口径。连续 20 票请求失败（如 EM 对出口 IP 风控）中止整批——否则
	 * 5572 票 × 每票重试退避在风控期会空转数小时。
	 * @return detail 标记（patched/unchanged/failed，中止时含 aborted）
	 */
	public Map<String, Object> patchEmIndustry() {
		int patched = 0;
		int unchanged = 0;
		int failed = 0;
		int consecutiveFailures = 0;
		boolean aborted = false;
		List<com.mx.nqboard.sniper.api.entity.StockBasicEntity> stocks = stockBasicService.list();
		for (com.mx.nqboard.sniper.api.entity.StockBasicEntity stock : stocks) {
			String emIndustry;
			try {
				emIndustry = eastmoneyClient.industryByCode(stock.getCode());
				consecutiveFailures = 0;
			}
			catch (RuntimeException e) {
				failed++;
				consecutiveFailures++;
				log.warn("industry patch failed for {}: {}", stock.getCode(), e.getMessage());
				if (consecutiveFailures >= 20) {
					log.error("industry patch aborted after {} consecutive failures", consecutiveFailures);
					aborted = true;
					break;
				}
				continue;
			}
			if (emIndustry == null || emIndustry.isBlank()) {
				unchanged++;
				continue;
			}
			if (!emIndustry.equals(stock.getIndustry()) || !"em".equals(stock.getIndustrySource())) {
				// 字符串列 UpdateWrapper（单测环境无 MP lambda 缓存，取舍同 DataGatewayImpl.fetchedToday）
				stockBasicService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<com.mx.nqboard.sniper.api.entity.StockBasicEntity>()
					.eq("code", stock.getCode())
					.set("industry", emIndustry)
					.set("industry_source", "em"));
				patched++;
			}
			else {
				unchanged++;
			}
		}
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("total", stocks.size());
		detail.put("patched", patched);
		detail.put("unchanged", unchanged);
		detail.put("failed", failed);
		if (aborted) {
			detail.put("aborted", true);
		}
		return detail;
	}

	Map<String, Object> runBackfill(LocalDate startDate, LocalDate endDate) {
		return dailyRunService.runGuarded(startDate, RunPhaseEnum.DATA_UPDATE, () -> {
			Map<String, Object> detail = new LinkedHashMap<>();
			try {
				// ① 股票基础全量 + 交易日历全量（各 1 调）
				detail.put("stockBasicRows", dataIngestService.ingestStockBasic());
				detail.put("calendarRows", dataIngestService.ingestTradeCalendar("19900101",
						LocalDate.now().plusDays(400).toString()));

				// ①' 东财 f127 行业修正跑批（§3.2 EM 现用口径；连续失败自动中止，不阻塞回填）
				detail.put("industryPatch", patchEmIndustry());

				// ② 逐交易日：none 行 + 因子（全市场各 1 调）
				List<LocalDate> tradingDays = dataGateway.getTradingDays(startDate, endDate);
				long totalNone = 0;
				long totalFactors = 0;
				for (LocalDate day : tradingDays) {
					Map<String, Object> dayDetail = dataIngestService.ingestDailyBars(day);
					totalNone += toLong(dayDetail.get("noneRows"));
					totalFactors += toLong(dayDetail.get("factorRows"));
				}
				detail.put("backfillDays", tradingDays.size());
				detail.put("totalNoneRows", totalNone);
				detail.put("totalFactorRows", totalFactors);

				// ③ 全市场 qfq 一次性合成（逐票库内重算；失败票计数不终止）
				int recalced = 0;
				int failed = 0;
				if (!tradingDays.isEmpty()) {
					List<String> codes = dataIngestService.listCodesWithFactorsOn(tradingDays.get(tradingDays.size() - 1));
					for (String code : codes) {
						try {
							if (dataIngestService.recalcQfqFull(code) > 0) {
								recalced++;
							}
						}
						catch (RuntimeException e) {
							failed++;
							log.warn("backfill qfq recalc failed for {}: {}", code, e.getMessage());
						}
					}
				}
				detail.put("qfqRecalcedCodes", recalced);
				detail.put("qfqFailedCodes", failed);
			}
			catch (RuntimeException e) {
				detail.put("backfillFailed", e.getMessage() == null ? e.toString() : e.getMessage());
				log.error("backfill {}~{} failed", startDate, endDate, e);
			}
			detail.put("budgetUsed", dailyBudget.used());
			return detail;
		});
	}

	/**
	 * qfq 抽样导出（bit-exact 验证门输入）：随机抽样 size 只，导出其 asOf 前 10 日 qfq 行
	 * 到 reports/qfq_sample_&lt;asOf&gt;.json，供 Python get_prices(adjust='qfq') 同窗口 diff（T15）。
	 * @return 导出文件路径
	 */
	public String exportQfqSample(int size, LocalDate asOf) {
		List<String> codes = dataIngestService.sampleCodes(size, asOf);
		Map<String, Object> sample = new LinkedHashMap<>();
		for (String code : codes) {
			List<KlineBar> bars = dataGateway.getPrices(code,
					asOf.minusDays(30), asOf, AdjustEnum.QFQ);
			List<Map<String, Object>> rows = bars.stream().map(b -> {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("date", b.tradeDate().toString());
				row.put("open", b.open() == null ? null : b.open().toPlainString());
				row.put("high", b.high() == null ? null : b.high().toPlainString());
				row.put("low", b.low() == null ? null : b.low().toPlainString());
				row.put("close", b.close() == null ? null : b.close().toPlainString());
				return row;
			}).toList();
			sample.put(code, rows);
		}
		try {
			Path dir = Path.of("reports");
			Files.createDirectories(dir);
			Path out = dir.resolve("qfq_sample_" + asOf + ".json");
			Files.writeString(out, new ObjectMapper().writeValueAsString(sample));
			log.info("qfq sample exported: {} codes -> {}", codes.size(), out);
			return out.toString();
		}
		catch (IOException e) {
			throw new IllegalStateException("qfq sample export failed: " + e.getMessage(), e);
		}
	}

	private static long toLong(Object value) {
		return value instanceof Number number ? number.longValue() : 0;
	}

}
