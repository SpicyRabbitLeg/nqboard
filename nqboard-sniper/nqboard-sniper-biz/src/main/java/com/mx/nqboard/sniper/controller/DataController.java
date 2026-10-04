package com.mx.nqboard.sniper.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.data.ingest.BackfillService;
import com.mx.nqboard.sniper.data.ingest.DailyUpdateService;
import com.mx.nqboard.sniper.service.DailyRunService;
import com.mx.nqboard.sniper.service.impl.DataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 数据资产查询与刷新触发（01 模块；Controller 只做参数组装——分页编排与过滤条件单一来源在
 * {@link DataQueryService}）。
 * 路径约定：Controller 的 @RequestMapping <b>不带 /sniper 前缀</b>（平台网关全局 StripPrefix=1，
 * 对齐 device /category 写法）。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "sniper-数据资产")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@RequestMapping("/data")
public class DataController {

	private final DataQueryService dataQueryService;

	private final DailyUpdateService dailyUpdateService;

	private final DailyRunService dailyRunService;

	private final BackfillService backfillService;

	// ------------------------------------------------------------------
	// 分页查询（13 张表；延迟关联范式）
	// ------------------------------------------------------------------

	@GetMapping("/daily-price")
	@Operation(summary = "日行情分页查询（code 与日期区间至少传一项，防全表拉取）")
	public R<Page<com.mx.nqboard.sniper.api.entity.DailyPriceEntity>> dailyPrice(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) AdjustEnum adjust, @RequestParam(required = false) String source) {
		return R.ok(dataQueryService.pageDailyPrice(current, size, code, start, end, adjust, source));
	}

	@GetMapping("/snapshot")
	@Operation(summary = "全市场快照按日分页查询（tradeDate 必填）")
	public R<Page<com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity>> snapshot(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(name = "changePctFrom", required = false) java.math.BigDecimal changePctFrom,
			@RequestParam(name = "changePctTo", required = false) java.math.BigDecimal changePctTo) {
		return R.ok(dataQueryService.pageSnapshot(current, size, tradeDate, keyword, changePctFrom, changePctTo));
	}

	@GetMapping("/news")
	@Operation(summary = "个股新闻分页查询（含预打标 sentiment/类型）")
	public R<Page<com.mx.nqboard.sniper.api.entity.CompanyNewsEntity>> news(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) String sentiment,
			@RequestParam(name = "announcementType", required = false) String announcementType) {
		return R.ok(dataQueryService.pageNews(current, size, code, start, end, sentiment, announcementType));
	}

	@GetMapping("/insider-trade")
	@Operation(summary = "股东增减持分页查询")
	public R<Page<com.mx.nqboard.sniper.api.entity.InsiderTradeEntity>> insiderTrade(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return R.ok(dataQueryService.pageInsiderTrade(current, size, code, start, end));
	}

	@GetMapping("/fund-flow")
	@Operation(summary = "主力资金流分页查询（单位：元）")
	public R<Page<com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity>> fundFlow(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return R.ok(dataQueryService.pageFundFlow(current, size, code, start, end));
	}

	@GetMapping("/dragon-tiger")
	@Operation(summary = "龙虎榜分页查询（tradeDate 全市场视图或 code+区间）")
	public R<Page<com.mx.nqboard.sniper.api.entity.DragonTigerEntity>> dragonTiger(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
			@RequestParam(required = false) String reason) {
		return R.ok(dataQueryService.pageDragonTiger(current, size, tradeDate, code, start, end, reason));
	}

	@GetMapping("/restricted-release")
	@Operation(summary = "限售解禁分页查询")
	public R<Page<com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity>> restrictedRelease(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return R.ok(dataQueryService.pageRestrictedRelease(current, size, code, start, end));
	}

	@GetMapping("/trade-calendar")
	@Operation(summary = "交易日历分页查询")
	public R<Page<com.mx.nqboard.sniper.api.entity.TradeCalendarEntity>> tradeCalendar(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
		return R.ok(dataQueryService.pageTradeCalendar(current, size, year, month));
	}

	@GetMapping("/stock-basic")
	@Operation(summary = "股票基础分页查询（含东财行业口径）")
	public R<Page<com.mx.nqboard.sniper.api.entity.StockBasicEntity>> stockBasic(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code, @RequestParam(required = false) String name,
			@RequestParam(name = "industrySource", required = false) String industrySource) {
		return R.ok(dataQueryService.pageStockBasic(current, size, code, name, industrySource));
	}

	@GetMapping("/constituents")
	@Operation(summary = "指数成分分页查询（最新快照日）")
	public R<Page<com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity>> constituents(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(name = "indexCode", required = false) String indexCode,
			@RequestParam(name = "snapshotDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
		return R.ok(dataQueryService.pageConstituents(current, size, indexCode, snapshotDate));
	}

	@GetMapping("/board-daily")
	@Operation(summary = "行业板块日K分页查询")
	public R<Page<com.mx.nqboard.sniper.api.entity.IndustryBoardDailyEntity>> boardDaily(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(name = "boardName", required = false) String boardName,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return R.ok(dataQueryService.pageBoardDaily(current, size, boardName, start, end));
	}

	@GetMapping("/financial")
	@Operation(summary = "财务指标分页查询（两腿按 source 区分展示）")
	public R<Page<com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity>> financial(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "reportPeriod", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportPeriod,
			@RequestParam(required = false) String source) {
		return R.ok(dataQueryService.pageFinancial(current, size, code, reportPeriod, source));
	}

	@GetMapping("/adj-factor")
	@Operation(summary = "复权因子分页查询")
	public R<Page<com.mx.nqboard.sniper.api.entity.AdjFactorEntity>> adjFactor(
			@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
			@RequestParam(required = false) String code,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return R.ok(dataQueryService.pageAdjFactor(current, size, code, start, end));
	}

	// ------------------------------------------------------------------
	// 数据刷新（手动触发与状态；Quartz data_update Job 调同一 DailyUpdateService）
	// ------------------------------------------------------------------

	@GetMapping("/refresh/status")
	@Operation(summary = "数据刷新状态：指定日期各阶段运行记录与预算消耗（默认当日）")
	public R<List<DailyRunEntity>> refreshStatus(
			@RequestParam(name = "runDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
		LocalDate date = runDate != null ? runDate : LocalDate.now();
		return R.ok(dailyRunService.lambdaQuery().eq(DailyRunEntity::getRunDate, date).list());
	}

	@PostMapping("/refresh/trigger")
	@SysLog("手动触发数据刷新")
	@HasPermission("sniper_data_refresh")
	@Operation(summary = "手动触发数据刷新（data_update=五阶段全序列；单阶段幂等可重跑）")
	public R<Map<String, Object>> refreshTrigger(
			@RequestParam(name = "runDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
			@RequestBody(required = false) RefreshTriggerBody body) {
		LocalDate date = runDate != null ? runDate : LocalDate.now();
		String phase = body != null && body.phase() != null && !body.phase().isBlank() ? body.phase() : "data_update";
		return switch (phase) {
			case "data_update" -> R.ok(dailyUpdateService.runDataUpdate(date));
			default -> R.failed("未知阶段：" + phase + "（支持 data_update）");
		};
	}

	/** 触发请求体（{phase: data_update}；单阶段触发随 T12 回填一并支持） */
	public record RefreshTriggerBody(String phase) {
	}

	@PostMapping("/backfill/run")
	@SysLog("手动触发历史回填")
	@HasPermission("sniper_data_refresh")
	@Operation(summary = "提交历史回填异步长任务（startDate~endDate 逐日全市场；进度看任务运行记录页）")
	public R<Boolean> backfillRun(@RequestBody BackfillRunBody body) {
		if (body == null || body.startDate() == null || body.endDate() == null) {
			return R.failed("startDate/endDate 必填");
		}
		// 前端二次确认由页面承担；任务进度经 sniper_daily_run（run_date=startDate, phase=data_update）可见
		boolean submitted = backfillService.submit(body.startDate(), body.endDate());
		return submitted ? R.ok(true) : R.failed("回填任务已在运行中，请稍后");
	}

	public record BackfillRunBody(
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	}

}
