package com.mx.nqboard.sniper.data.ingest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity;
import com.mx.nqboard.sniper.api.entity.InsiderTradeEntity;
import com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EventIngestService 转换语义单测——万元→元、DE 取负、ann_date 兜底链、float_ratio ÷100 同值近似。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class EventIngestServiceTest {

	private final TushareClient tushare = mock(TushareClient.class);

	private final CompositeProvider composite = mock(CompositeProvider.class);

	private final SniperUpsertMapper upsert = mock(SniperUpsertMapper.class);

	private final EventIngestService service = new EventIngestService(tushare, composite, upsert);

	private static TushareRow row(List<String> fields, List<Object> values) {
		Map<String, com.fasterxml.jackson.databind.JsonNode> cells = new java.util.LinkedHashMap<>();
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		for (int i = 0; i < fields.size(); i++) {
			cells.put(fields.get(i), mapper.valueToTree(values.get(i)));
		}
		return new TushareRow(cells);
	}

	@Test
	@DisplayName("资金流：万元×1e4→元（main_net 阈值按元硬编码），买卖差值列同口径")
	void fundFlowWanToYuan() {
		when(tushare.moneyflow(anyString(), anyString(), anyString())).thenReturn(List.of(
				row(List.of("ts_code", "trade_date", "net_mf_amount", "buy_elg_amount", "sell_elg_amount",
						"buy_lg_amount", "sell_lg_amount", "buy_md_amount", "sell_md_amount", "buy_sm_amount",
						"sell_sm_amount"),
						List.of("600519.SH", "20260930", "123.45", "200", "50", "80", "30", "10", "5", "1", "2"))));
		when(upsert.upsertFundFlowDaily(any())).thenReturn(1);

		int n = service.ingestFundFlow("600519", LocalDate.of(2026, 9, 30));

		assertThat(n).isEqualTo(1);
		ArgumentCaptor<List<FundFlowDailyEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertFundFlowDaily(captor.capture());
		FundFlowDailyEntity e = captor.getValue().get(0);
		// 123.45 万元 → 1_234_500 元
		assertThat(e.getMainNet()).isEqualByComparingTo("1234500");
		// super = (200−50) 万 → 1_500_000 元
		assertThat(e.getSuperNet()).isEqualByComparingTo("1500000");
		assertThat(e.getLargeNet()).isEqualByComparingTo("500000");
		assertThat(e.getMediumNet()).isEqualByComparingTo("50000");
		assertThat(e.getSmallNet()).isEqualByComparingTo("-10000");
	}

	@Test
	@DisplayName("增减持：in_de=DE 时 change_vol 取负；ann_date 空则 demat_date 兜底再退 end_date")
	void insiderDeNegativeAndAnnDateFallback() {
		when(tushare.stkHoldertrade(anyString(), anyString(), anyString())).thenReturn(List.of(
				row(List.of("ts_code", "ann_date", "holder_name", "holder_type", "in_de", "change_vol",
						"avg_price", "after_share"),
						// Arrays.asList 允许 null（ann_date 为空走 demat_date→end_date 兜底链）
						java.util.Arrays.asList("600519.SH", null, "张三", "高管", "DE", "600", "1800.0", "10000")),
				row(List.of("ts_code", "ann_date", "holder_name", "holder_type", "in_de", "change_vol",
						"avg_price", "after_share"),
						List.of("600519.SH", "20260920", "李四", "股东", "IN", "300", "1750.0", "20000"))));
		when(upsert.upsertInsiderTrade(any())).thenReturn(2);
		LocalDate end = LocalDate.of(2026, 9, 30);

		int n = service.ingestInsiderTrades("600519", end);

		assertThat(n).isEqualTo(2);
		ArgumentCaptor<List<InsiderTradeEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertInsiderTrade(captor.capture());
		List<InsiderTradeEntity> entities = captor.getValue();
		// DE 减持取负（-600），ann_date 兜底退 end_date
		assertThat(entities.get(0).getChangeVol()).isEqualByComparingTo("-600");
		assertThat(entities.get(0).getAnnDate()).isEqualTo(end);
		// IN 增持保持正数
		assertThat(entities.get(1).getChangeVol()).isEqualByComparingTo("300");
		assertThat(entities.get(1).getAnnDate()).isEqualTo(LocalDate.of(2026, 9, 20));
	}

	@Test
	@DisplayName("解禁：float_ratio 百分点÷100 且同值近似 float_mv_ratio；查询失败 fail-open 返回 0")
	void restrictedRatioNormalizationAndFailOpen() {
		when(tushare.shareFloat(anyString(), anyString(), anyString())).thenReturn(List.of(
				row(List.of("ts_code", "float_date", "float_share", "float_ratio"),
						List.of("600519.SH", "20261010", "5000000", "8.0"))));
		when(upsert.upsertRestrictedRelease(any())).thenReturn(1);

		service.ingestRestrictedRelease("600519", LocalDate.of(2026, 9, 30));

		ArgumentCaptor<List<RestrictedReleaseEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(upsert).upsertRestrictedRelease(captor.capture());
		RestrictedReleaseEntity e = captor.getValue().get(0);
		assertThat(e.getFloatRatio()).isEqualByComparingTo(new BigDecimal("0.08"));
		// 同值近似：tushare 占总股本口径写入 float_mv_ratio（更小更保守）
		assertThat(e.getFloatMvRatio()).isEqualByComparingTo(new BigDecimal("0.08"));

		// fail-open：主源异常 → 返回 0 不抛
		when(tushare.shareFloat(anyString(), anyString(), anyString()))
			.thenThrow(new com.mx.nqboard.sniper.data.provider.tushare.TushareClientException("boom"));
		assertThat(service.ingestRestrictedRelease("600519", LocalDate.of(2026, 9, 30))).isZero();
	}

	@Test
	@DisplayName("财务两腿：腿1 百分比÷100 存小数；腿2 total_mv 万元×1e4→元，两腿 source 区分")
	void financialTwoLegsUnits() {
		when(tushare.finaIndicator(anyString(), anyString(), anyString())).thenReturn(List.of(
				row(List.of("end_date", "grossprofit_margin", "netprofit_margin", "roe", "or_yoy", "netprofit_yoy"),
						List.of("20260630", "91.19", "45.6", "12.3", "8.8", "15.5"))));
		when(tushare.dailyBasic(anyString(), anyString(), anyString())).thenReturn(List.of(
				row(List.of("trade_date", "pe_ttm", "pb", "ps_ttm", "total_mv"),
						List.of("20260930", "25.5", "8.1", "6.2", "2100000"))));
		when(upsert.upsertFinancialIndicator(any())).thenReturn(1);

		service.ingestFinancialFundamentals("600519", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30));
		service.ingestFinancialValuation("600519", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

		ArgumentCaptor<List<com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity>> captor =
				ArgumentCaptor.forClass(List.class);
		verify(upsert, org.mockito.Mockito.times(2)).upsertFinancialIndicator(captor.capture());
		List<List<com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity>> calls = captor.getAllValues();
		// 腿1：91.19% → 0.9119
		assertThat(calls.get(0).get(0).getGrossMargin()).isEqualByComparingTo("0.9119");
		assertThat(calls.get(0).get(0).getRoe()).isEqualByComparingTo("0.123");
		assertThat(calls.get(0).get(0).getSource()).isEqualTo("ths");
		// 腿2：total_mv 2100000 万 → 210 亿元
		assertThat(calls.get(1).get(0).getMarketCap()).isEqualByComparingTo("21000000000");
		assertThat(calls.get(1).get(0).getPeTtm()).isEqualByComparingTo("25.5");
		assertThat(calls.get(1).get(0).getSource()).isEqualTo("tushare");
	}

}
