package com.mx.nqboard.sniper.data.ingest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.api.enums.RunStatusEnum;
import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.service.AdjFactorService;
import com.mx.nqboard.sniper.service.DailyRunService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DailyUpdateService 编排单测——五阶段缺数标记不终止、行情失败跳过除权、预算写入 detail。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
class DailyUpdateServiceTest {

	private final DataIngestService dataIngest = mock(DataIngestService.class);

	private final EventIngestService eventIngest = mock(EventIngestService.class);

	private final AdjFactorService adjFactorService = mock(AdjFactorService.class);

	private final DailyRunService dailyRunService = mock(DailyRunService.class);

	private final DailyBudget budget = new DailyBudget(300);

	private final DailyUpdateService service;

	{
		// runGuarded 打桩为真实执行 work（否则编排逻辑不在被测路径上）
		when(dailyRunService.runGuarded(any(), any(), any())).thenAnswer(inv -> {
			java.util.function.Supplier<Map<String, Object>> work = inv.getArgument(2);
			return work.get();
		});
	}

	DailyUpdateServiceTest() {
		com.mx.nqboard.sniper.data.DataGateway dataGateway = mock(com.mx.nqboard.sniper.data.DataGateway.class);
		// 默认回退到入参自身（交易日），个别用例覆盖
		when(dataGateway.resolveAsOf(any())).thenAnswer(inv -> inv.getArgument(0));
		service = new DailyUpdateService(dataIngest, eventIngest, adjFactorService, dailyRunService, budget,
				dataGateway);
	}

	@Test
	@DisplayName("五阶段全成功：detail 聚合各段行数与预算消耗")
	void aggregatesAllStages() {
		when(dataIngest.ingestSpot(any(), any())).thenReturn(5400);
		when(dataIngest.ingestTradeCalendar(any(), any())).thenReturn(407);
		when(dataIngest.ingestDailyAndFactors(any()))
			.thenReturn(Map.of("noneRows", 5400, "factorRows", 5400, "qfqRows", 5400));
		when(dataIngest.ingestIndexDaily(any(), any(), any())).thenReturn(30);
		when(eventIngest.ingestDragonTiger(any())).thenReturn(25);
		// 除权检测：当日/前日因子一致 → 0 重算
		mockFactors("2026-09-30", "5.0", "2026-09-29", "5.0");

		Map<String, Object> detail = service.runDataUpdate(LocalDate.of(2026, 9, 30));

		assertThat(detail.get("spotRows")).isEqualTo(5400);
		assertThat(detail.get("noneRows")).isEqualTo(5400);
		assertThat(detail.get("exDividendRecalcs")).isEqualTo(0);
		assertThat(detail.get("dragonRows")).isEqualTo(25);
		assertThat(detail.get("budgetUsed")).isEqualTo(0);
		// 经 runGuarded 包装
		verify(dailyRunService, times(1)).runGuarded(eq(LocalDate.of(2026, 9, 30)), eq(RunPhaseEnum.DATA_UPDATE),
				any());
	}

	@Test
	@DisplayName("阶段失败记缺数标记不终止：快照/龙虎榜失败后续阶段照跑")
	void stageFailureDoesNotAbort() {
		when(dataIngest.ingestSpot(any(), any())).thenThrow(new RuntimeException("spot down"));
		when(dataIngest.ingestTradeCalendar(any(), any())).thenReturn(0);
		when(dataIngest.ingestDailyAndFactors(any()))
			.thenReturn(Map.of("noneRows", 5400, "factorRows", 5400, "qfqRows", 5400));
		when(eventIngest.ingestDragonTiger(any())).thenReturn(25);
		mockFactors("2026-09-30", "5.0", "2026-09-29", "5.0");

		Map<String, Object> detail = service.runDataUpdate(LocalDate.of(2026, 9, 30));

		assertThat(detail.get("spotFailed")).isEqualTo("spot down");
		assertThat(detail.get("noneRows")).isEqualTo(5400);
		assertThat(detail.get("dragonRows")).isEqualTo(25);
	}

	@Test
	@DisplayName("行情失败：跳过除权检测（qfq 依赖当日 none+因子），事件照跑")
	void dailyFailureSkipsExDividend() {
		when(dataIngest.ingestSpot(any(), any())).thenReturn(0);
		when(dataIngest.ingestTradeCalendar(any(), any())).thenReturn(0);
		when(dataIngest.ingestDailyAndFactors(any())).thenThrow(new RuntimeException("tushare down"));
		when(eventIngest.ingestDragonTiger(any())).thenReturn(0);

		Map<String, Object> detail = service.runDataUpdate(LocalDate.of(2026, 9, 30));

		assertThat(detail.get("dailyFailed")).isEqualTo("tushare down");
		assertThat(detail).doesNotContainKey("exDividendRecalcs");
		assertThat(detail.get("dragonRows")).isEqualTo(0);
	}

	@Test
	@DisplayName("除权检测：当日因子≠前日 → 变化票重算；前日因子空（回填未做）→ 跳过")
	void exDividendDetectionGuards() {
		// 前日因子空 → 0（防全市场误重算）
		mockFactors("2026-09-30", "5.0", null, null);
		assertThat(service.detectAndRecalcExDividend(LocalDate.of(2026, 9, 30))).isZero();

		// 因子变化 → 重算（latestTradingDayBefore 走 getOne：返回 0929 行）
		AdjFactorEntity today = factor("600519", "2026-09-30", "5.0");
		AdjFactorEntity prev = factor("600519", "2026-09-29", "4.0");
		AdjFactorEntity todayOther = factor("000001", "2026-09-30", "2.0");
		AdjFactorEntity prevOther = factor("000001", "2026-09-29", "2.0");
		when(adjFactorService.getOne(any(), eq(false))).thenReturn(prev);
		when(adjFactorService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
			.thenReturn(List.of(today, todayOther))
			.thenReturn(List.of(prev, prevOther));
		when(dataIngest.recalcQfqFull("600519")).thenReturn(8000);

		int recalcs = service.detectAndRecalcExDividend(LocalDate.of(2026, 9, 30));

		assertThat(recalcs).isEqualTo(8000);
		verify(dataIngest, org.mockito.Mockito.times(1)).recalcQfqFull(any());
	}

	private void mockFactors(String todayDate, String todayFactor, String prevDate, String prevFactor) {
		AdjFactorEntity t = todayDate == null ? null
				: factor("600519", todayDate, todayFactor);
		AdjFactorEntity p = prevDate == null ? null : factor("600519", prevDate, prevFactor);
		when(adjFactorService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(t == null ? List.of() : List.of(t))
			.thenReturn(p == null ? List.of() : List.of(p));
	}

	private static AdjFactorEntity factor(String code, String date, String value) {
		AdjFactorEntity e = new AdjFactorEntity();
		e.setCode(code);
		e.setTradeDate(LocalDate.parse(date));
		e.setFactor(new java.math.BigDecimal(value));
		return e;
	}

}
