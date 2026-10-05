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

	private final com.mx.nqboard.sniper.data.provider.em.EastmoneyClient eastmoneyClient =
			mock(com.mx.nqboard.sniper.data.provider.em.EastmoneyClient.class);

	private final com.mx.nqboard.sniper.service.StockBasicService stockBasicService =
			mock(com.mx.nqboard.sniper.service.StockBasicService.class);

	private final BackfillService service = new BackfillService(dataIngest, dailyRunService, dataGateway,
			new DailyBudget(300), eastmoneyClient, stockBasicService);

	{
		// runGuarded 打桩为真实执行 work（回填编排逻辑在被测路径上）
		when(dailyRunService.runGuarded(any(), any(), any())).thenAnswer(inv -> {
			java.util.function.Supplier<Map<String, Object>> work = inv.getArgument(2);
			return work.get();
		});
		// ①' 行业修正跑批在回填路径上执行；默认空表（行业修正单测见 patchEmIndustry 相关用例）
		when(stockBasicService.list()).thenReturn(List.of());
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
		// 单线程 executor 串行：verify(times(1)) 在 runGuarded 被调用的瞬间即通过，而 backfilling
		// 复位发生在任务 finally 里稍晚一点——轮询提交直到首任务结束放行（原写法存在竞态偶发失败）
		verify(dailyRunService, timeout(2000).times(1)).runGuarded(eq(LocalDate.of(2026, 9, 28)),
				eq(com.mx.nqboard.sniper.api.enums.RunPhaseEnum.DATA_UPDATE), any());
		boolean second = false;
		for (int i = 0; i < 100 && !second; i++) {
			second = service.submit(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30));
			if (!second) {
				Thread.sleep(10);
			}
		}
		assertThat(second).isTrue();
		verify(dailyRunService, timeout(2000).times(2)).runGuarded(any(), any(), any());
	}

	@Test
	@DisplayName("f127 行业修正：非空且差异才覆盖置 'em'，空值保持 tushare 口径")
	void patchEmIndustryOverwrites() {
		com.mx.nqboard.sniper.api.entity.StockBasicEntity s1 = stock("600519", "白酒(旧)", "tushare");
		com.mx.nqboard.sniper.api.entity.StockBasicEntity s2 = stock("000001", null, null);
		com.mx.nqboard.sniper.api.entity.StockBasicEntity s3 = stock("600036", "银行", "em");
		when(stockBasicService.list()).thenReturn(List.of(s1, s2, s3));
		when(eastmoneyClient.industryByCode("600519")).thenReturn("白酒");
		when(eastmoneyClient.industryByCode("000001")).thenReturn(null);
		when(eastmoneyClient.industryByCode("600036")).thenReturn("银行");

		Map<String, Object> detail = service.patchEmIndustry();

		assertThat(detail.get("total")).isEqualTo(3);
		assertThat(detail.get("patched")).isEqualTo(1);
		assertThat(detail.get("unchanged")).isEqualTo(2);
		assertThat(detail.get("failed")).isEqualTo(0);
		verify(stockBasicService).update(any());
	}

	@Test
	@DisplayName("f127 行业修正：连续 20 票请求失败中止整批（防风控期空转）")
	void patchEmIndustryAbortsOnConsecutiveFailures() {
		List<com.mx.nqboard.sniper.api.entity.StockBasicEntity> stocks = new java.util.ArrayList<>();
		for (int i = 0; i < 30; i++) {
			stocks.add(stock("6000" + String.format("%02d", i), null, null));
		}
		when(stockBasicService.list()).thenReturn(stocks);
		when(eastmoneyClient.industryByCode(anyString())).thenThrow(new IllegalStateException("em blocked"));

		Map<String, Object> detail = service.patchEmIndustry();

		assertThat(detail.get("aborted")).isEqualTo(true);
		assertThat(detail.get("failed")).isEqualTo(20);
		verify(eastmoneyClient, times(20)).industryByCode(anyString());
	}

	private static com.mx.nqboard.sniper.api.entity.StockBasicEntity stock(String code, String industry,
			String industrySource) {
		com.mx.nqboard.sniper.api.entity.StockBasicEntity e = new com.mx.nqboard.sniper.api.entity.StockBasicEntity();
		e.setCode(code);
		e.setIndustry(industry);
		e.setIndustrySource(industrySource);
		return e;
	}

}
