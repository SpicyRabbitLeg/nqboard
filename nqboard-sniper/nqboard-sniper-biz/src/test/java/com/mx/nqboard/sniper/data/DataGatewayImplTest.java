package com.mx.nqboard.sniper.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.api.entity.TradeCalendarEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.ingest.DataIngestService;
import com.mx.nqboard.sniper.data.ingest.EventIngestService;
import com.mx.nqboard.sniper.data.model.SecurityStatus;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.service.CompanyNewsService;
import com.mx.nqboard.sniper.service.DailyPriceService;
import com.mx.nqboard.sniper.service.StockBasicService;
import com.mx.nqboard.sniper.service.TradeCalendarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
 * DataGatewayImpl 读序单测——库足额不发请求、缺口拉取回写、冷却判定、限价计算、行业修正。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
class DataGatewayImplTest {

	private final TradeCalendarService tradeCalendarService = mock(TradeCalendarService.class);

	private final StockBasicService stockBasicService = mock(StockBasicService.class);

	private final DailyPriceService dailyPriceService = mock(DailyPriceService.class);

	private final CompanyNewsService companyNewsService = mock(CompanyNewsService.class);

	private final DataIngestService dataIngest = mock(DataIngestService.class);

	private final EventIngestService eventIngest = mock(EventIngestService.class);

	private final CompositeProvider composite = mock(CompositeProvider.class);

	@SuppressWarnings("unchecked")
	private final BaseMapper<com.mx.nqboard.sniper.api.entity.CompanyNewsEntity> newsMapper =
			mock(BaseMapper.class);

	private final DataGatewayImpl gateway = new DataGatewayImpl(tradeCalendarService, stockBasicService,
			mock(com.mx.nqboard.sniper.service.IndexConstituentsService.class), dailyPriceService,
			mock(com.mx.nqboard.sniper.service.IndexDailyService.class),
			mock(com.mx.nqboard.sniper.service.AdjFactorService.class),
			mock(com.mx.nqboard.sniper.service.MarketSnapshotService.class),
			mock(com.mx.nqboard.sniper.service.IndustryBoardDailyService.class),
			mock(com.mx.nqboard.sniper.service.FinancialIndicatorService.class), companyNewsService,
			mock(com.mx.nqboard.sniper.service.InsiderTradeService.class),
			mock(com.mx.nqboard.sniper.service.FundFlowDailyService.class),
			mock(com.mx.nqboard.sniper.service.DragonTigerService.class),
			mock(com.mx.nqboard.sniper.service.RestrictedReleaseService.class), dataIngest, eventIngest, composite);

	private static TradeCalendarEntity openDay(LocalDate date) {
		TradeCalendarEntity e = new TradeCalendarEntity();
		e.setCalDate(date);
		e.setIsOpen(1);
		return e;
	}

	private static DailyPriceEntity noneBar(LocalDate date, String preClose) {
		DailyPriceEntity e = new DailyPriceEntity();
		e.setCode("600519");
		e.setTradeDate(date);
		e.setAdjust(AdjustEnum.NONE);
		e.setPreClose(preClose == null ? null : new BigDecimal(preClose));
		e.setClose(preClose == null ? null : new BigDecimal(preClose));
		e.setSource("tushare");
		return e;
	}

	@Test
	@DisplayName("getPrices：库内行数≥交易日数 → 不触发缺口拉取")
	void pricesServedFromStoreWhenComplete() {
		LocalDate start = LocalDate.of(2026, 9, 28);
		LocalDate end = LocalDate.of(2026, 9, 30);
		List<TradeCalendarEntity> calendar = List.of(openDay(start), openDay(start.plusDays(1)), openDay(end));
		when(tradeCalendarService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(calendar);
		when(dailyPriceService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
			.thenReturn(List.of(noneBar(start, "10.0"), noneBar(start.plusDays(1), "10.1"), noneBar(end, "10.2")));

		List<com.mx.nqboard.sniper.data.model.KlineBar> bars = gateway.getPrices("600519", start, end,
				AdjustEnum.NONE);

		assertThat(bars).hasSize(3);
		verify(dataIngest, never()).fillPriceGap(anyString(), anyString(), any(), any(), any());
	}

	@Test
	@DisplayName("getPrices：缺口 → fillPriceGap 回写 → 重查返回")
	void pricesFillsGapWhenIncomplete() {
		LocalDate start = LocalDate.of(2026, 9, 28);
		LocalDate end = LocalDate.of(2026, 9, 30);
		when(tradeCalendarService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(openDay(start), openDay(start.plusDays(1)),
				openDay(end)));
		// 第一次查只有 2 行（缺 1 天）→ 拉取 → 第二次查 3 行
		when(dailyPriceService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(noneBar(start, "10.0"), noneBar(end, "10.2")))
			.thenReturn(List.of(noneBar(start, "10.0"), noneBar(start.plusDays(1), "10.1"), noneBar(end, "10.2")));

		List<com.mx.nqboard.sniper.data.model.KlineBar> bars = gateway.getPrices("600519", start, end,
				AdjustEnum.NONE);

		assertThat(bars).hasSize(3);
		verify(dataIngest, times(1)).fillPriceGap(eq("600519"), anyString(), eq(start), eq(end),
				eq(AdjustEnum.NONE));
	}

	@Test
	@DisplayName("resolveAsOf：非交易日回退上一交易日；日历空退化为周末回退")
	void resolveAsOfFallsBackToLastTradingDay() {
		// 周六（非交易日）：isTradingDay list 空 → getOne 返回周五
		when(tradeCalendarService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
		TradeCalendarEntity friday = openDay(LocalDate.of(2026, 9, 25));
		when(tradeCalendarService.getOne(any(), eq(false))).thenReturn(friday);

		LocalDate saturday = LocalDate.of(2026, 9, 26);
		assertThat(gateway.resolveAsOf(saturday)).isEqualTo(LocalDate.of(2026, 9, 25));
	}

	@Test
	@DisplayName("getSecurityStatus：限价=pre_close×(1±ratio) 不四舍五入到分；主板 10%")
	void securityStatusComputesLimitPrices() {
		LocalDate asOf = LocalDate.of(2026, 9, 30);
		// isTradingDay=true；basic 非 ST；none 行 pre_close=10.00
		when(tradeCalendarService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(openDay(asOf)));
		when(stockBasicService.getOne(any(), eq(false))).thenReturn(basic("贵州茅台", "贵州茅台", "tushare"));
		when(dailyPriceService.getOne(any(), eq(false))).thenReturn(noneBar(asOf, "10.00"));

		SecurityStatus status = gateway.getSecurityStatus("600519", asOf);

		assertThat(status.suspended()).isFalse();
		assertThat(status.limitRatio()).isEqualByComparingTo("0.10");
		// 不四舍五入到 2 位：10.00×1.1=11.0000（交易所挂单价四舍五入由 M2 比较容差吸收，照 Python 语义）
		assertThat(status.limitUp()).isEqualByComparingTo("11.0000");
		assertThat(status.limitDown()).isEqualByComparingTo("9.0000");
	}

	@Test
	@DisplayName("getSecurityStatus：ST 限比 0.05；非交易日 suspended=true")
	void securityStatusStAndSuspended() {
		LocalDate asOf = LocalDate.of(2026, 10, 3); // 周六
		when(tradeCalendarService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of()); // 非交易日
		when(stockBasicService.getOne(any(), eq(false))).thenReturn(basic("ST某某", "ST某某", "tushare"));

		SecurityStatus status = gateway.getSecurityStatus("600519", asOf);

		assertThat(status.suspended()).isTrue();
		assertThat(status.isSt()).isTrue();
		assertThat(status.limitRatio()).isEqualByComparingTo("0.05");
	}

	@Test
	@DisplayName("getCompanyNews：当日已拉（冷却命中）→ 不重复发外部请求")
	void newsCooldownSkipsRefetch() {
		when(companyNewsService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
		@SuppressWarnings("unchecked")
		BaseMapper<com.mx.nqboard.sniper.api.entity.CompanyNewsEntity> newsMapper = Mockito
			.mock(BaseMapper.class);
		when(companyNewsService.getBaseMapper()).thenReturn(newsMapper);
		when(newsMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

		gateway.getCompanyNews("600519", LocalDate.of(2026, 9, 30), null, 50);

		verify(eventIngest, never()).ingestNews(anyString(), org.mockito.ArgumentMatchers.anyInt());
	}

	@Test
	@DisplayName("getStockIndustry：em 口径直返；tushare 口径经 EM 修正回写")
	void industryPrefersEmAndBackfills() {
		// em 口径直返
		when(stockBasicService.getOne(any(), eq(false))).thenReturn(basic("贵州茅台", "电池", "em"));
		assertThat(gateway.getStockIndustry("600519")).isEqualTo("电池");
		verify(stockBasicService, never()).updateById(any());

		// tushare 旧口径 → EM f127 修正回写
		when(stockBasicService.getOne(any(), eq(false))).thenReturn(basic("贵州茅台", "电气设备", "tushare"));
		when(composite.industry(eq("600519"), any())).thenReturn("电池");

		String industry = gateway.getStockIndustry("600519");

		assertThat(industry).isEqualTo("电池");
		verify(stockBasicService, times(1)).updateById(any());
	}

	private static StockBasicEntity basic(String name, String industry, String source) {
		StockBasicEntity e = new StockBasicEntity();
		e.setCode("600519");
		e.setName(name);
		e.setIndustry(industry);
		e.setIndustrySource(source);
		return e;
	}

}
