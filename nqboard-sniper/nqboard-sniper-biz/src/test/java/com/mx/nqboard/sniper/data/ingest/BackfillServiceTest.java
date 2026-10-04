package com.mx.nqboard.sniper.data.ingest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.data.DataGateway;
import com.mx.nqboard.sniper.service.DailyRunService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BackfillService 编排单测——逐日 none+因子、全市场 qfq 重算聚合、失败票计数不终止、异步提交。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
class BackfillServiceTest {

	private final DataIngestService dataIngest = mock(DataIngestService.class);

	private final DailyRunService dailyRunService = mock(DailyRunService.class);

	private final DataGateway dataGateway = mock(DataGateway.class);

	private final BackfillService service = new BackfillService(dataIngest, dailyRunService, dataGateway,
			new DailyBudget(300));

	{
		// runGuarded 打桩为真实执行 work（回填编排逻辑在被测路径上）
		when(dailyRunService.runGuarded(any(), any(), any())).thenAnswer(inv -> {
			java.util.function.Supplier<Map<String, Object>> work = inv.getArgument(2);
			return work.get();
		});
	}

	@Test
	@DisplayName("回填编排：基础全量→逐日两段→全市场 qfq 重算聚合，detail 汇总计数")
	void runBackfillAggregates() {
		LocalDate start = LocalDate.of(2026, 9, 28);
		LocalDate end = LocalDate.of(2026, 9, 30);
		List<LocalDate> days = List.of(start, start.plusDays(1), end);
		when(dataGateway.getTradingDays(start, end)).thenReturn(days);
		when(dataIngest.ingestStockBasic()).thenReturn(5400);
		when(dataIngest.ingestTradeCalendar(anyString(), anyString())).thenReturn(9000);
		when(dataIngest.ingestDailyBars(any())).thenReturn(Map.of("noneRows", 5400, "factorRows", 5400));
		when(dataIngest.listCodesWithFactorsOn(end)).thenReturn(List.of("600519", "000001"));
		when(dataIngest.recalcQfqFull("600519")).thenReturn(8000);
		when(dataIngest.recalcQfqFull("000001")).thenReturn(0); // 无 none 行（未上市等）→ 计 0

		Map<String, Object> detail = service.runBackfill(start, end);

		assertThat(detail.get("backfillDays")).isEqualTo(3);
		assertThat(detail.get("totalNoneRows")).isEqualTo(16200L);
		assertThat(detail.get("qfqRecalcedCodes")).isEqualTo(1);
		verify(dataIngest, times(3)).ingestDailyBars(any());
		verify(dataIngest, times(2)).recalcQfqFull(anyString());
	}

	@Test
	@DisplayName("单票重算失败计数不终止（failed 票继续）")
	void recalcFailureCountedNotAbort() {
		LocalDate start = LocalDate.of(2026, 9, 30);
		when(dataGateway.getTradingDays(start, start)).thenReturn(List.of(start));
		when(dataIngest.ingestStockBasic()).thenReturn(0);
		when(dataIngest.ingestTradeCalendar(anyString(), anyString())).thenReturn(0);
		when(dataIngest.ingestDailyBars(any())).thenReturn(Map.of("noneRows", 5400, "factorRows", 5400));
		when(dataIngest.listCodesWithFactorsOn(start)).thenReturn(List.of("600519", "000001"));
		when(dataIngest.recalcQfqFull("600519")).thenThrow(new RuntimeException("db down"));
		when(dataIngest.recalcQfqFull("000001")).thenReturn(100);

		Map<String, Object> detail = service.runBackfill(start, start);

		assertThat(detail.get("qfqRecalcedCodes")).isEqualTo(1);
		assertThat(detail.get("qfqFailedCodes")).isEqualTo(1);
		verify(dataIngest, times(2)).recalcQfqFull(anyString());
	}

	@Test
	@DisplayName("submit 异步提交成功执行 runGuarded；运行中重复提交被拒")
	void submitAsyncAndRejectDuplicate() throws Exception {
		when(dataGateway.getTradingDays(any(), any())).thenReturn(List.of());
		when(dataIngest.ingestStockBasic()).thenReturn(0);
		when(dataIngest.ingestTradeCalendar(anyString(), anyString())).thenReturn(0);

		boolean first = service.submit(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30));
		assertThat(first).isTrue();
		// 单线程 executor 串行：第二次提交在首个任务结束前可能被拒（AtomicBoolean 语义），结束后允许
		verify(dailyRunService, timeout(2000).times(1)).runGuarded(eq(LocalDate.of(2026, 9, 28)),
				eq(com.mx.nqboard.sniper.api.enums.RunPhaseEnum.DATA_UPDATE), any());
		// 首任务完成后 backfilling 已复位 → 可再次提交
		boolean second = service.submit(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30));
		assertThat(second).isTrue();
		verify(dailyRunService, timeout(2000).times(2)).runGuarded(any(), any(), any());
	}

}
