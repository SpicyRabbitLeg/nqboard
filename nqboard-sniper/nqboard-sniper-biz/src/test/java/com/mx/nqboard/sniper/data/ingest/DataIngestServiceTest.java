package com.mx.nqboard.sniper.data.ingest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataIngestService 编排单测——日更三段 upsert、qfq 合成公式（价×factor÷max，只乘价格列）、
 * 成分快照日过滤、除权全历史重算。Mockito 全 mock，不触库。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class DataIngestServiceTest {

	private final TushareClient tushare = mock(TushareClient.class);

	private final CompositeProvider composite = mock(CompositeProvider.class);

	private final SniperUpsertMapper upsert = mock(SniperUpsertMapper.class);

	private final DataIngestService service = new DataIngestService(tushare, composite, upsert);

	private static TushareRow row(List<String> fields, List<Object> values) {
		java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> cells = new java.util.LinkedHashMap<>();
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		for (int i = 0; i < fields.size(); i++) {
			cells.put(fields.get(i), mapper.valueToTree(values.get(i)));
		}
		return new TushareRow(cells);
	}

	@Test
	@DisplayName("日更：none/factor/qfq 三段 upsert，qfq=none×factor÷max 且只乘价格列")
	void ingestDailyAndFactorsSynthesizesQfq() {
		LocalDate date = LocalDate.of(2026, 9, 30);
		List<String> priceFields = List.of("ts_code", "trade_date", "open", "high", "low", "close", "vol",
				"amount", "pre_close");
		when(tushare.dailyByTradeDate("20260930")).thenReturn(List.of(
				row(priceFields, List.of("600519.SH", "20260930", "10", "10.5", "9.8", "10.2", "12345", "5678",
						"9.9"))));
		when(tushare.adjFactorByTradeDate("20260930")).thenReturn(List.of(
				row(List.of("ts_code", "trade_date", "adj_factor"),
						List.of("600519.SH", "20260930", "5.0"))));
		// 全历史 max（含当日 5.0 之前已到 10.0）→ qfq = none × 5 ÷ 10
		when(upsert.selectMaxFactor("600519")).thenReturn(BigDecimal.TEN);
		// T12 拆分后：synthQfqForDate 从库读当日 none 行与因子（mock 查库桩）
		DailyPriceEntity storedNone = new DailyPriceEntity();
		storedNone.setId(99L);
		storedNone.setCode("600519");
		storedNone.setTradeDate(date);
		storedNone.setAdjust(com.mx.nqboard.sniper.api.enums.AdjustEnum.NONE);
		storedNone.setOpen(new BigDecimal("10"));
		storedNone.setHigh(new BigDecimal("10.5"));
		storedNone.setLow(new BigDecimal("9.8"));
		storedNone.setClose(new BigDecimal("10.2"));
		storedNone.setPreClose(new BigDecimal("9.9"));
		storedNone.setVolume(new BigDecimal("12345"));
		storedNone.setAmount(new BigDecimal("5678"));
		storedNone.setSource("tushare");
		AdjFactorEntity storedFactor = new AdjFactorEntity();
		storedFactor.setCode("600519");
		storedFactor.setTradeDate(date);
		storedFactor.setFactor(new BigDecimal("5.0"));
		when(upsert.selectNoneBarsByDate(date)).thenReturn(List.of(storedNone));
		when(upsert.selectFactorsByDate(date)).thenReturn(List.of(storedFactor));
		when(upsert.upsertDailyPrice(any())).thenReturn(1);
		when(upsert.upsertAdjFactor(any())).thenReturn(1);

		Map<String, Object> detail = service.ingestDailyAndFactors(date);

		assertThat(detail.get("noneRows")).isEqualTo(1);
		assertThat(detail.get("factorRows")).isEqualTo(1);
		assertThat(detail.get("qfqRows")).isEqualTo(1);

		ArgumentCaptor<List<DailyPriceEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert, times(2)).upsertDailyPrice(captor.capture());
		List<List<DailyPriceEntity>> calls = captor.getAllValues();
		// 第一次：none 行（单位原样：amount 千元、pre_close 保留）
		assertThat(calls.get(0).get(0).getAdjust()).isEqualTo(com.mx.nqboard.sniper.api.enums.AdjustEnum.NONE);
		assertThat(calls.get(0).get(0).getAmount()).isEqualByComparingTo("5678");
		assertThat(calls.get(0).get(0).getPreClose()).isEqualByComparingTo("9.9");
		// 第二次：qfq 行——close = 10.2×5.0÷10 = 5.1（scale 4）；价格列乘、量额列不乘、pre_close 置空（附录 A #20②）
		DailyPriceEntity qfq = calls.get(1).get(0);
		assertThat(qfq.getAdjust()).isEqualTo(com.mx.nqboard.sniper.api.enums.AdjustEnum.QFQ);
		assertThat(qfq.getClose()).isEqualByComparingTo("5.1000");
		assertThat(qfq.getOpen()).isEqualByComparingTo("5.0000");
		assertThat(qfq.getVolume()).isEqualByComparingTo("12345");
		assertThat(qfq.getAmount()).isEqualByComparingTo("5678");
		assertThat(qfq.getPreClose()).isNull();
		assertThat(qfq.getId()).isNotNull();
	}

	@Test
	@DisplayName("成分快照：只取 max(trade_date) 全部行，con_code 校验 6 位")
	void ingestIndexConstituentsTakesLatestSnapshotOnly() {
		List<String> fields = List.of("index_code", "trade_date", "con_code");
		when(tushare.indexWeight("000300.SH")).thenReturn(List.of(
				row(fields, List.of("000300.SH", "20260929", "000001.SZ")),
				row(fields, List.of("000300.SH", "20260930", "600519.SH")),
				row(fields, List.of("000300.SH", "20260930", "12AB.SZ"))));
		when(upsert.upsertIndexConstituents(any())).thenReturn(1);

		int n = service.ingestIndexConstituents("000300.SH");

		// 旧快照日(0929)剔除、非法码(12AB)剔除 → 仅 600519 一行
		assertThat(n).isEqualTo(1);
		ArgumentCaptor<List<com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity>> captor =
				ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertIndexConstituents(captor.capture());
		assertThat(captor.getValue().get(0).getSnapshotDate()).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(captor.getValue().get(0).getCode()).isEqualTo("600519");
	}

	@Test
	@DisplayName("除权重算：库内 none+因子全历史重算 qfq 并覆盖 upsert")
	void recalcQfqFullRecalculatesFromStoredBars() {
		when(upsert.selectNoneBars("600519")).thenReturn(List.of(
				noneBar(LocalDate.of(2026, 9, 29), "10.0"),
				noneBar(LocalDate.of(2026, 9, 30), "10.2")));
		when(upsert.selectFactors("600519")).thenReturn(List.of(
				factor(LocalDate.of(2026, 9, 29), "4.0"),
				factor(LocalDate.of(2026, 9, 30), "5.0")));
		when(upsert.upsertDailyPrice(any())).thenReturn(2);

		int n = service.recalcQfqFull("600519");

		assertThat(n).isEqualTo(2);
		ArgumentCaptor<List<DailyPriceEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertDailyPrice(captor.capture());
		List<DailyPriceEntity> qfqBars = captor.getValue();
		// max=5.0：9-29 qfq=10.0×4÷5=8.0；9-30 qfq=10.2×5÷5=10.2——除权后全历史平移
		assertThat(qfqBars.get(0).getClose()).isEqualByComparingTo("8.0000");
		assertThat(qfqBars.get(1).getClose()).isEqualByComparingTo("10.2000");
	}

	@Test
	@DisplayName("行情缺口：CompositeProvider 4 级链结果按请求口径入库，maxFactor 供给方接库内点查")
	void fillPriceGapConsumesCompositeChain() {
		when(composite.prices(eq("600519"), eq("SH"), any(LocalDate.class), any(LocalDate.class),
				eq(com.mx.nqboard.sniper.api.enums.AdjustEnum.QFQ), any()))
			.thenReturn(List.of(new com.mx.nqboard.sniper.data.model.KlineBar("600519",
					LocalDate.of(2026, 9, 30), new BigDecimal("10"), new BigDecimal("10.5"),
					new BigDecimal("9.8"), new BigDecimal("10.2"), new BigDecimal("12345"), null, null, "em")));
		when(upsert.upsertDailyPrice(any())).thenReturn(1);

		int n = service.fillPriceGap("600519", "SH", LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30),
				com.mx.nqboard.sniper.api.enums.AdjustEnum.QFQ);

		assertThat(n).isEqualTo(1);
		ArgumentCaptor<List<DailyPriceEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertDailyPrice(captor.capture());
		assertThat(captor.getValue().get(0).getSource()).isEqualTo("em");
		assertThat(captor.getValue().get(0).getAdjust())
			.isEqualTo(com.mx.nqboard.sniper.api.enums.AdjustEnum.QFQ);
	}

	private static DailyPriceEntity noneBar(LocalDate date, String close) {
		DailyPriceEntity e = new DailyPriceEntity();
		e.setCode("600519");
		e.setTradeDate(date);
		e.setAdjust(com.mx.nqboard.sniper.api.enums.AdjustEnum.NONE);
		e.setOpen(new BigDecimal(close));
		e.setHigh(new BigDecimal(close));
		e.setLow(new BigDecimal(close));
		e.setClose(new BigDecimal(close));
		e.setVolume(new BigDecimal("100"));
		e.setSource("tushare");
		return e;
	}

	private static AdjFactorEntity factor(LocalDate date, String value) {
		AdjFactorEntity e = new AdjFactorEntity();
		e.setCode("600519");
		e.setTradeDate(date);
		e.setFactor(new BigDecimal(value));
		return e;
	}

}
