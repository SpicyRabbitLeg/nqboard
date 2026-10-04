package com.mx.nqboard.sniper.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.CompanyNewsEntity;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.api.entity.DragonTigerEntity;
import com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity;
import com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity;
import com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity;
import com.mx.nqboard.sniper.api.entity.InsiderTradeEntity;
import com.mx.nqboard.sniper.api.entity.IndustryBoardDailyEntity;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.api.entity.TradeCalendarEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.ingest.DataIngestService;
import com.mx.nqboard.sniper.data.ingest.EventIngestService;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.SecurityStatus;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.service.AdjFactorService;
import com.mx.nqboard.sniper.service.CompanyNewsService;
import com.mx.nqboard.sniper.service.DailyPriceService;
import com.mx.nqboard.sniper.service.DragonTigerService;
import com.mx.nqboard.sniper.service.FinancialIndicatorService;
import com.mx.nqboard.sniper.service.FundFlowDailyService;
import com.mx.nqboard.sniper.service.IndexConstituentsService;
import com.mx.nqboard.sniper.service.InsiderTradeService;
import com.mx.nqboard.sniper.service.IndustryBoardDailyService;
import com.mx.nqboard.sniper.service.MarketSnapshotService;
import com.mx.nqboard.sniper.service.RestrictedReleaseService;
import com.mx.nqboard.sniper.service.StockBasicService;
import com.mx.nqboard.sniper.service.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * DataGateway 实现——读序：MySQL → 缺口拉取（经 DataIngestService/EventIngestService 回写）→ 重查返回。
 * 事件类表（新闻/增减持/资金流/解禁/板块K）用<b>冷却判定</b>防重复拉取：该票当日（fetched_at≥当日零点）
 * 已拉过则不再发外部请求——同时解决"0 行无法区分未拉过"的问题（无解禁是大多数票的正常态）。
 * 行情类缺口用"区间行数 vs 交易日数"精确判定。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataGatewayImpl implements DataGateway {

	private static final Pattern ST_NAME = Pattern.compile("^\\*?ST", Pattern.CASE_INSENSITIVE);

	/** 腿1 报告期字段（fina_indicator 替代源，主文档 §4.3.7） */
	private final TradeCalendarService tradeCalendarService;

	private final StockBasicService stockBasicService;

	private final IndexConstituentsService indexConstituentsService;

	private final DailyPriceService dailyPriceService;

	private final AdjFactorService adjFactorService;

	private final MarketSnapshotService marketSnapshotService;

	private final IndustryBoardDailyService industryBoardDailyService;

	private final FinancialIndicatorService financialIndicatorService;

	private final CompanyNewsService companyNewsService;

	private final InsiderTradeService insiderTradeService;

	private final FundFlowDailyService fundFlowDailyService;

	private final DragonTigerService dragonTigerService;

	private final RestrictedReleaseService restrictedReleaseService;

	private final DataIngestService dataIngestService;

	private final EventIngestService eventIngestService;

	private final CompositeProvider compositeProvider;

	// ------------------------------------------------------------------
	// 行情
	// ------------------------------------------------------------------

	@Override
	public List<KlineBar> getPrices(String code, LocalDate start, LocalDate end, AdjustEnum adjust) {
		List<DailyPriceEntity> stored = queryPrices(code, start, end, adjust);
		long expected = getTradingDays(start, end).size();
		if (stored.size() >= expected || expected == 0) {
			return toBars(stored);
		}
		// 缺口拉取回写（4 级链 / Tushare 合成），再重查返回
		dataIngestService.fillPriceGap(code, exchangeOf(code), start, end, adjust);
		stored = queryPrices(code, start, end, adjust);
		return toBars(stored);
	}

	@Override
	public List<KlineBar> getIndexPrices(String indexCode6, LocalDate start, LocalDate end) {
		List<DailyPriceEntity> stored = queryPrices(indexCode6, start, end, AdjustEnum.NONE);
		if (!stored.isEmpty()) {
			return toBars(stored);
		}
		dataIngestService.ingestIndexDaily(indexCode6, start, end);
		return toBars(queryPrices(indexCode6, start, end, AdjustEnum.NONE));
	}

	@Override
	public List<MarketSnapshotEntity> getSpotSnapshot(LocalDate asOf) {
		List<MarketSnapshotEntity> stored = marketSnapshotService.list(Wrappers
			.<MarketSnapshotEntity>lambdaQuery().eq(MarketSnapshotEntity::getTradeDate, asOf));
		if (!stored.isEmpty()) {
			return stored;
		}
		dataIngestService.ingestSpot(asOf, tencentSymbols());
		return marketSnapshotService
			.list(Wrappers.<MarketSnapshotEntity>lambdaQuery().eq(MarketSnapshotEntity::getTradeDate, asOf));
	}

	// ------------------------------------------------------------------
	// 日历与基础
	// ------------------------------------------------------------------

	@Override
	public List<LocalDate> getTradingDays(LocalDate start, LocalDate end) {
		List<LocalDate> days = tradeCalendarService
			.list(Wrappers.<TradeCalendarEntity>lambdaQuery()
				.eq(TradeCalendarEntity::getIsOpen, 1)
				.ge(TradeCalendarEntity::getCalDate, start)
				.le(TradeCalendarEntity::getCalDate, end)
				.orderByAsc(TradeCalendarEntity::getCalDate))
			.stream()
			.map(TradeCalendarEntity::getCalDate)
			.toList();
		if (!days.isEmpty()) {
			return days;
		}
		// 日历表空退化为周一~周五近似（calendar.py 同款兜底，仅日志告警）
		log.warn("trade calendar empty; falling back to weekday approximation ({}~{})", start, end);
		List<LocalDate> approx = new ArrayList<>();
		for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
			if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
				approx.add(d);
			}
		}
		return approx;
	}

	@Override
	public LocalDate resolveAsOf(LocalDate raw) {
		if (isTradingDay(raw)) {
			return raw;
		}
		TradeCalendarEntity lastOpen = tradeCalendarService.getOne(Wrappers.<TradeCalendarEntity>lambdaQuery()
			.eq(TradeCalendarEntity::getIsOpen, 1)
			.le(TradeCalendarEntity::getCalDate, raw)
			.orderByDesc(TradeCalendarEntity::getCalDate)
			.last("LIMIT 1"), false);
		LocalDate asOf = lastOpen == null ? null : lastOpen.getCalDate();
		if (asOf != null) {
			return asOf;
		}
		// 表空退化：周末回退到周五
		log.warn("trade calendar empty; resolveAsOf fallback to weekday");
		LocalDate d = raw;
		while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
			d = d.minusDays(1);
		}
		return d;
	}

	private boolean isTradingDay(LocalDate date) {
		return !tradeCalendarService.list(Wrappers.<TradeCalendarEntity>lambdaQuery()
			.eq(TradeCalendarEntity::getCalDate, date)
			.eq(TradeCalendarEntity::getIsOpen, 1)).isEmpty();
	}

	@Override
	public List<IndexConstituentsEntity> getIndexConstituents(String indexCode) {
		IndexConstituentsEntity latestRow = indexConstituentsService
			.getOne(Wrappers.<IndexConstituentsEntity>lambdaQuery()
				.eq(IndexConstituentsEntity::getIndexCode, indexCode)
				.orderByDesc(IndexConstituentsEntity::getSnapshotDate)
				.last("LIMIT 1"), false);
		LocalDate latest = latestRow == null ? null : latestRow.getSnapshotDate();
		if (latest == null) {
			return List.of();
		}
		return indexConstituentsService.list(Wrappers.<IndexConstituentsEntity>lambdaQuery()
			.eq(IndexConstituentsEntity::getIndexCode, indexCode)
			.eq(IndexConstituentsEntity::getSnapshotDate, latest));
	}

	@Override
	public StockBasicEntity getStockBasic(String code) {
		return stockBasicService.getOne(Wrappers.<StockBasicEntity>lambdaQuery()
			.eq(StockBasicEntity::getCode, code), false);
	}

	@Override
	public Map<String, BigDecimal> getAdjFactors(LocalDate tradeDate) {
		Map<String, BigDecimal> factors = new LinkedHashMap<>();
		for (AdjFactorEntity e : adjFactorService.list(Wrappers.<AdjFactorEntity>lambdaQuery()
			.eq(AdjFactorEntity::getTradeDate, tradeDate))) {
			factors.put(e.getCode(), e.getFactor());
		}
		return factors;
	}

	// ------------------------------------------------------------------
	// 财务与事件（冷却判定防重复拉取）
	// ------------------------------------------------------------------

	@Override
	public List<FinancialIndicatorEntity> getFinancialMetrics(String code, LocalDate asOf, int limit) {
		// 两腿各取 asOf 前最新 limit 行（PIT 过滤放消费侧，§4.3.7）
		List<FinancialIndicatorEntity> merged = new ArrayList<>();
		merged.addAll(financialIndicatorService.list(Wrappers.<FinancialIndicatorEntity>lambdaQuery()
			.eq(FinancialIndicatorEntity::getCode, code)
			.eq(FinancialIndicatorEntity::getSource, "ths")
			.le(FinancialIndicatorEntity::getReportPeriod, asOf)
			.orderByDesc(FinancialIndicatorEntity::getReportPeriod)
			.last("LIMIT " + Math.max(limit, 1))));
		merged.addAll(financialIndicatorService.list(Wrappers.<FinancialIndicatorEntity>lambdaQuery()
			.eq(FinancialIndicatorEntity::getCode, code)
			.eq(FinancialIndicatorEntity::getSource, "tushare")
			.le(FinancialIndicatorEntity::getReportPeriod, asOf)
			.orderByDesc(FinancialIndicatorEntity::getReportPeriod)
			.last("LIMIT " + Math.max(limit, 1))));
		merged.sort((a, b) -> b.getReportPeriod().compareTo(a.getReportPeriod()));
		return merged;
	}

	@Override
	public List<CompanyNewsEntity> getCompanyNews(String code, LocalDate asOf, LocalDate start, int limit) {
		List<CompanyNewsEntity> stored = companyNewsService.list(Wrappers.<CompanyNewsEntity>lambdaQuery()
			.eq(CompanyNewsEntity::getCode, code)
			.le(CompanyNewsEntity::getPublishedAt, asOf.plusDays(1).atStartOfDay())
			.ge(start != null, CompanyNewsEntity::getPublishedAt, start == null ? null : start.atStartOfDay())
			.orderByDesc(CompanyNewsEntity::getPublishedAt)
			.last("LIMIT " + Math.max(limit, 1)));
		if (fetchedToday(code, companyNewsService.getBaseMapper())) {
			return stored;
		}
		// 该票当日未拉过 → 按需拉取（fetch_date 增量游标），0 条仅告警不拒票
		eventIngestService.ingestNews(code, limit);
		return companyNewsService.list(Wrappers.<CompanyNewsEntity>lambdaQuery()
			.eq(CompanyNewsEntity::getCode, code)
			.le(CompanyNewsEntity::getPublishedAt, asOf.plusDays(1).atStartOfDay())
			.orderByDesc(CompanyNewsEntity::getPublishedAt)
			.last("LIMIT " + Math.max(limit, 1)));
	}

	@Override
	public List<InsiderTradeEntity> getInsiderTrades(String code, LocalDate asOf, int limit) {
		List<InsiderTradeEntity> stored = insiderTradeService.list(Wrappers.<InsiderTradeEntity>lambdaQuery()
			.eq(InsiderTradeEntity::getCode, code)
			.le(InsiderTradeEntity::getAnnDate, asOf)
			.orderByDesc(InsiderTradeEntity::getAnnDate)
			.last("LIMIT " + Math.max(limit, 1)));
		if (fetchedToday(code, insiderTradeService.getBaseMapper())) {
			return stored;
		}
		eventIngestService.ingestInsiderTrades(code, asOf);
		return insiderTradeService.list(Wrappers.<InsiderTradeEntity>lambdaQuery()
			.eq(InsiderTradeEntity::getCode, code)
			.le(InsiderTradeEntity::getAnnDate, asOf)
			.orderByDesc(InsiderTradeEntity::getAnnDate)
			.last("LIMIT " + Math.max(limit, 1)));
	}

	@Override
	public List<FundFlowDailyEntity> getMainFundFlow(String code, LocalDate asOf, int days) {
		List<FundFlowDailyEntity> stored = fundFlowDailyService.list(Wrappers.<FundFlowDailyEntity>lambdaQuery()
			.eq(FundFlowDailyEntity::getCode, code)
			.le(FundFlowDailyEntity::getTradeDate, asOf)
			.orderByDesc(FundFlowDailyEntity::getTradeDate)
			.last("LIMIT " + Math.max(days, 1)));
		if (fetchedToday(code, fundFlowDailyService.getBaseMapper())) {
			return stored;
		}
		eventIngestService.ingestFundFlow(code, asOf);
		return fundFlowDailyService.list(Wrappers.<FundFlowDailyEntity>lambdaQuery()
			.eq(FundFlowDailyEntity::getCode, code)
			.le(FundFlowDailyEntity::getTradeDate, asOf)
			.orderByDesc(FundFlowDailyEntity::getTradeDate)
			.last("LIMIT " + Math.max(days, 1)));
	}

	@Override
	public List<DragonTigerEntity> getDragonTiger(String code, LocalDate asOf, int days) {
		if (!fetchedToday(code, dragonTigerService.getBaseMapper()) && isTradingDay(asOf)) {
			// 当日全市场 top_list 未拉过 → 拉 1 调（比逐票区间省 ~30 倍）
			eventIngestService.ingestDragonTiger(asOf);
		}
		return dragonTigerService.list(Wrappers.<DragonTigerEntity>lambdaQuery()
			.eq(DragonTigerEntity::getCode, code)
			.ge(DragonTigerEntity::getTradeDate, asOf.minusDays(days))
			.le(DragonTigerEntity::getTradeDate, asOf)
			.orderByDesc(DragonTigerEntity::getTradeDate));
	}

	@Override
	public List<RestrictedReleaseEntity> getRestrictedRelease(String code, LocalDate asOf) {
		List<RestrictedReleaseEntity> stored = restrictedReleaseService
			.list(Wrappers.<RestrictedReleaseEntity>lambdaQuery().eq(RestrictedReleaseEntity::getCode, code));
		if (fetchedToday(code, restrictedReleaseService.getBaseMapper())) {
			return stored;
		}
		// fail-open：查询失败返回空列表，不拦截信号（gate 语义）
		eventIngestService.ingestRestrictedRelease(code, asOf);
		return restrictedReleaseService
			.list(Wrappers.<RestrictedReleaseEntity>lambdaQuery().eq(RestrictedReleaseEntity::getCode, code));
	}

	@Override
	public String getStockIndustry(String code) {
		StockBasicEntity basic = getStockBasic(code);
		if (basic == null) {
			return null;
		}
		if ("em".equals(basic.getIndustrySource()) && basic.getIndustry() != null) {
			return basic.getIndustry();
		}
		// 库内 tushare 旧口径（或空）→ EM f127 修正回写（与板块名对齐的现用口径）
		String emIndustry = compositeProvider.industry(code, fallback -> basic.getIndustry());
		if (emIndustry != null && !emIndustry.equals(basic.getIndustry())) {
			StockBasicEntity patch = new StockBasicEntity();
			patch.setId(basic.getId());
			patch.setIndustry(emIndustry);
			patch.setIndustrySource("em");
			patch.setUpdateBy("sniper");
			patch.setUpdateTime(LocalDateTime.now());
			stockBasicService.updateById(patch);
			return emIndustry;
		}
		return basic.getIndustry();
	}

	@Override
	public List<IndustryBoardDailyEntity> getIndustryBoardHist(String boardName, LocalDate asOf) {
		List<IndustryBoardDailyEntity> stored = industryBoardDailyService
			.list(Wrappers.<IndustryBoardDailyEntity>lambdaQuery()
				.eq(IndustryBoardDailyEntity::getBoardName, boardName)
				.le(IndustryBoardDailyEntity::getTradeDate, asOf)
				.orderByDesc(IndustryBoardDailyEntity::getTradeDate));
		if (!stored.isEmpty()) {
			return stored;
		}
		// deep 票 sector_rotation 按需拉近 120 日回写（日更任务不必全量刷 90+ 板块）
		dataIngestService.ingestBoardDaily(boardName, asOf.minusDays(120), asOf);
		return industryBoardDailyService.list(Wrappers.<IndustryBoardDailyEntity>lambdaQuery()
			.eq(IndustryBoardDailyEntity::getBoardName, boardName)
			.le(IndustryBoardDailyEntity::getTradeDate, asOf)
			.orderByDesc(IndustryBoardDailyEntity::getTradeDate));
	}

	@Override
	public SecurityStatus getSecurityStatus(String code, LocalDate asOf) {
		boolean suspended = !isTradingDay(asOf);
		StockBasicEntity basic = getStockBasic(code);
		boolean isSt = basic != null && basic.getName() != null && ST_NAME.matcher(basic.getName()).find();
		BigDecimal ratio = SecurityStatus.ratioOf(code, isSt);
		// 涨跌停价：base=asOf 当日交易所发布前收盘（tushare pre_close，除权日已是调整后值）——不复权路径
		DailyPriceEntity bar = dailyPriceService.getOne(Wrappers.<DailyPriceEntity>lambdaQuery()
			.eq(DailyPriceEntity::getCode, code)
			.eq(DailyPriceEntity::getTradeDate, asOf)
			.eq(DailyPriceEntity::getAdjust, AdjustEnum.NONE)
			.last("LIMIT 1"), false);
		if (bar == null) {
			// 当日 none 行缺失 → 缺口拉取（停牌检查显式 adjust=none 语义）
			dataIngestService.fillPriceGap(code, exchangeOf(code), asOf, asOf, AdjustEnum.NONE);
			bar = dailyPriceService.getOne(Wrappers.<DailyPriceEntity>lambdaQuery()
				.eq(DailyPriceEntity::getCode, code)
				.eq(DailyPriceEntity::getTradeDate, asOf)
				.eq(DailyPriceEntity::getAdjust, AdjustEnum.NONE)
				.last("LIMIT 1"), false);
		}
		if (bar == null || bar.getPreClose() == null || bar.getPreClose().signum() <= 0) {
			// 交易日无 bar 属 inconclusive：保守放行（不判停牌，源码同款语义）；限价不可算置 null
			return new SecurityStatus(code, asOf, suspended, null, null, ratio, isSt);
		}
		BigDecimal base = bar.getPreClose();
		// 不四舍五入（源码语义，配合 is_limit_locked 的 0.01 容差比较）
		BigDecimal limitUp = base.multiply(BigDecimal.ONE.add(ratio)).setScale(4, RoundingMode.HALF_UP);
		BigDecimal limitDown = base.multiply(BigDecimal.ONE.subtract(ratio)).setScale(4, RoundingMode.HALF_UP);
		return new SecurityStatus(code, asOf, suspended, limitUp, limitDown, ratio, isSt);
	}

	@Override
	public List<Object> getNorthboundHoldings(String code, LocalDate asOf) {
		// 港交所 2024-08-16 起停止逐日披露，数据源已失效（P-12/B.1 #7）——恒空，M2 预检据此告警
		log.debug("northbound holdings discontinued since 2024-08-16; returning empty for {}", code);
		return List.of();
	}

	// ------------------------------------------------------------------
	// 私有段
	// ------------------------------------------------------------------

	private List<DailyPriceEntity> queryPrices(String code, LocalDate start, LocalDate end, AdjustEnum adjust) {
		return dailyPriceService.list(Wrappers.<DailyPriceEntity>lambdaQuery()
			.eq(DailyPriceEntity::getCode, code)
			.eq(DailyPriceEntity::getAdjust, adjust)
			.ge(DailyPriceEntity::getTradeDate, start)
			.le(DailyPriceEntity::getTradeDate, end)
			.orderByAsc(DailyPriceEntity::getTradeDate));
	}

	private static List<KlineBar> toBars(List<DailyPriceEntity> entities) {
		return entities.stream()
			.map(e -> new KlineBar(e.getCode(), e.getTradeDate(), e.getOpen(), e.getHigh(), e.getLow(),
					e.getClose(), e.getVolume(), e.getAmount(), e.getPreClose(), e.getSource()))
			.toList();
	}

	/** 冷却判定：该票 fetched_at≥当日零点（今天已拉过）→ 不再发外部请求（字符串列 + 具体 mapper，无 lambda 解析） */
	private <T extends com.mx.nqboard.common.mybatis.base.BaseEntity> boolean fetchedToday(String code,
			com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper) {
		return mapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<T>()
			.eq("code", code)
			.ge("fetched_at", LocalDate.now().atStartOfDay())) > 0;
	}

	/** 腾讯批量报价代码表（快照兜底级依赖；从 stock_basic 全表生成 sh/sz 前缀代码） */
	private List<String> tencentSymbols() {
		return stockBasicService.list().stream()
			.map(b -> b.getExchange().toLowerCase() + b.getCode())
			.toList();
	}

	private String exchangeOf(String code) {
		StockBasicEntity basic = getStockBasic(code);
		if (basic != null && basic.getExchange() != null) {
			return basic.getExchange();
		}
		// 无 basic 记录按代码段推断（与 toTsCode 同规则）
		if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
			return "SH";
		}
		if (code.startsWith("4") || code.startsWith("8")) {
			return "BJ";
		}
		return "SZ";
	}

}
