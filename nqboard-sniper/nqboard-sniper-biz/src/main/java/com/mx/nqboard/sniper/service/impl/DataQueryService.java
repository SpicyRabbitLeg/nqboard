package com.mx.nqboard.sniper.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
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
import com.mx.nqboard.sniper.mapper.AdjFactorMapper;
import com.mx.nqboard.sniper.mapper.CompanyNewsMapper;
import com.mx.nqboard.sniper.mapper.DailyPriceMapper;
import com.mx.nqboard.sniper.mapper.DragonTigerMapper;
import com.mx.nqboard.sniper.mapper.FinancialIndicatorMapper;
import com.mx.nqboard.sniper.mapper.FundFlowDailyMapper;
import com.mx.nqboard.sniper.mapper.IndexConstituentsMapper;
import com.mx.nqboard.sniper.mapper.InsiderTradeMapper;
import com.mx.nqboard.sniper.mapper.IndustryBoardDailyMapper;
import com.mx.nqboard.sniper.mapper.MarketSnapshotMapper;
import com.mx.nqboard.sniper.mapper.RestrictedReleaseMapper;
import com.mx.nqboard.sniper.mapper.StockBasicMapper;
import com.mx.nqboard.sniper.mapper.TradeCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 数据资产查询聚合服务（13 张行情/事件表的分页查询；平台<b>延迟关联范式</b>：
 * 先分页查 id 防深分页全表回表 → 按 id 批查详情 → IN 不保序按页序内存回填；
 * 过滤条件 build*Wrapper 单一来源；无业务排序时 orderByDesc(id) 兜底保证分页稳定）。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Service
@RequiredArgsConstructor
public class DataQueryService {

	private static final int MAX_PAGE_SIZE = 500;

	private final TradeCalendarMapper tradeCalendarMapper;

	private final StockBasicMapper stockBasicMapper;

	private final IndexConstituentsMapper indexConstituentsMapper;

	private final DailyPriceMapper dailyPriceMapper;

	private final AdjFactorMapper adjFactorMapper;

	private final MarketSnapshotMapper marketSnapshotMapper;

	private final IndustryBoardDailyMapper industryBoardDailyMapper;

	private final FinancialIndicatorMapper financialIndicatorMapper;

	private final CompanyNewsMapper companyNewsMapper;

	private final InsiderTradeMapper insiderTradeMapper;

	private final FundFlowDailyMapper fundFlowDailyMapper;

	private final DragonTigerMapper dragonTigerMapper;

	private final RestrictedReleaseMapper restrictedReleaseMapper;

	/**
	 * 延迟关联模板：idWrapper 由调用方 build（含过滤、排序）；idGetter 为实体 id 取值
	 * （BaseEntity 无 id 字段，各实体自声明）。
	 */
	public <T extends BaseEntity> Page<T> pageDelayed(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
			LambdaQueryWrapper<T> idWrapper, Function<T, Long> idGetter, long current, long size) {
		long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Page<T> page = mapper.selectPage(new Page<>(current, safeSize), idWrapper);
		List<T> idRows = page.getRecords();
		if (idRows.isEmpty()) {
			page.setRecords(List.of());
			return page;
		}
		List<Long> ids = idRows.stream().map(idGetter).toList();
		Map<Long, T> detailMap = mapper.selectBatchIds(ids).stream()
			.collect(Collectors.toMap(idGetter, Function.identity()));
		page.setRecords(ids.stream().map(detailMap::get).filter(Objects::nonNull).toList());
		return page;
	}

	public Page<TradeCalendarEntity> pageTradeCalendar(long current, long size, Integer year, Integer month) {
		LambdaQueryWrapper<TradeCalendarEntity> wrapper = new LambdaQueryWrapper<TradeCalendarEntity>()
			.apply(year != null, "YEAR(cal_date) = {0}", year)
			.apply(month != null, "MONTH(cal_date) = {0}", month)
			.orderByDesc(TradeCalendarEntity::getId);
		return pageDelayed(tradeCalendarMapper, wrapper, TradeCalendarEntity::getId, current, size);
	}

	public Page<StockBasicEntity> pageStockBasic(long current, long size, String code, String name,
			String industrySource) {
		LambdaQueryWrapper<StockBasicEntity> wrapper = new LambdaQueryWrapper<StockBasicEntity>()
			.eq(code != null && !code.isBlank(), StockBasicEntity::getCode, code)
			.like(name != null && !name.isBlank(), StockBasicEntity::getName, name)
			.eq(industrySource != null && !industrySource.isBlank(), StockBasicEntity::getIndustrySource,
					industrySource)
			.orderByDesc(StockBasicEntity::getId);
		return pageDelayed(stockBasicMapper, wrapper, StockBasicEntity::getId, current, size);
	}

	public Page<IndexConstituentsEntity> pageConstituents(long current, long size, String indexCode,
			LocalDate snapshotDate) {
		LambdaQueryWrapper<IndexConstituentsEntity> wrapper = new LambdaQueryWrapper<IndexConstituentsEntity>()
			.eq(indexCode != null && !indexCode.isBlank(), IndexConstituentsEntity::getIndexCode, indexCode)
			.eq(snapshotDate != null, IndexConstituentsEntity::getSnapshotDate, snapshotDate)
			.orderByDesc(IndexConstituentsEntity::getId);
		return pageDelayed(indexConstituentsMapper, wrapper, IndexConstituentsEntity::getId, current, size);
	}

	/** 强制筛选：code 与日期区间至少一项，防全表拉取（页面规范） */
	public Page<DailyPriceEntity> pageDailyPrice(long current, long size, String code, LocalDate start,
			LocalDate end, AdjustEnum adjust, String source) {
		if ((code == null || code.isBlank()) && start == null && end == null) {
			throw new IllegalArgumentException("code 与日期区间至少传一项（防全表拉取）");
		}
		LambdaQueryWrapper<DailyPriceEntity> wrapper = new LambdaQueryWrapper<DailyPriceEntity>()
			.eq(code != null && !code.isBlank(), DailyPriceEntity::getCode, code)
			.ge(start != null, DailyPriceEntity::getTradeDate, start)
			.le(end != null, DailyPriceEntity::getTradeDate, end)
			.eq(adjust != null, DailyPriceEntity::getAdjust, adjust)
			.eq(source != null && !source.isBlank(), DailyPriceEntity::getSource, source)
			.orderByDesc(DailyPriceEntity::getTradeDate, DailyPriceEntity::getId);
		return pageDelayed(dailyPriceMapper, wrapper, DailyPriceEntity::getId, current, size);
	}

	public Page<AdjFactorEntity> pageAdjFactor(long current, long size, String code, LocalDate start,
			LocalDate end) {
		LambdaQueryWrapper<AdjFactorEntity> wrapper = new LambdaQueryWrapper<AdjFactorEntity>()
			.eq(code != null && !code.isBlank(), AdjFactorEntity::getCode, code)
			.ge(start != null, AdjFactorEntity::getTradeDate, start)
			.le(end != null, AdjFactorEntity::getTradeDate, end)
			.orderByDesc(AdjFactorEntity::getTradeDate, AdjFactorEntity::getId);
		return pageDelayed(adjFactorMapper, wrapper, AdjFactorEntity::getId, current, size);
	}

	/** 强制筛选：tradeDate 必填（快照按日查询） */
	public Page<MarketSnapshotEntity> pageSnapshot(long current, long size, LocalDate tradeDate, String keyword,
			BigDecimal changePctFrom, BigDecimal changePctTo) {
		if (tradeDate == null) {
			throw new IllegalArgumentException("tradeDate 必填（快照按日查询）");
		}
		LambdaQueryWrapper<MarketSnapshotEntity> wrapper = new LambdaQueryWrapper<MarketSnapshotEntity>()
			.eq(MarketSnapshotEntity::getTradeDate, tradeDate)
			.and(keyword != null && !keyword.isBlank(),
					w -> w.like(MarketSnapshotEntity::getCode, keyword).or()
						.like(MarketSnapshotEntity::getName, keyword))
			.ge(changePctFrom != null, MarketSnapshotEntity::getChangePct, changePctFrom)
			.le(changePctTo != null, MarketSnapshotEntity::getChangePct, changePctTo)
			.orderByDesc(MarketSnapshotEntity::getChangePct, MarketSnapshotEntity::getId);
		return pageDelayed(marketSnapshotMapper, wrapper, MarketSnapshotEntity::getId, current, size);
	}

	public Page<CompanyNewsEntity> pageNews(long current, long size, String code, LocalDate start, LocalDate end,
			String sentiment, String announcementType) {
		LambdaQueryWrapper<CompanyNewsEntity> wrapper = new LambdaQueryWrapper<CompanyNewsEntity>()
			.eq(code != null && !code.isBlank(), CompanyNewsEntity::getCode, code)
			.ge(start != null, CompanyNewsEntity::getPublishedAt, start == null ? null : start.atStartOfDay())
			.le(end != null, CompanyNewsEntity::getPublishedAt, end == null ? null : end.plusDays(1).atStartOfDay())
			.eq(sentiment != null && !sentiment.isBlank(), CompanyNewsEntity::getSentiment,
					sentiment == null ? null : sentiment.toUpperCase())
			.eq(announcementType != null && !announcementType.isBlank(), CompanyNewsEntity::getAnnouncementType,
					announcementType)
			.orderByDesc(CompanyNewsEntity::getPublishedAt, CompanyNewsEntity::getId);
		return pageDelayed(companyNewsMapper, wrapper, CompanyNewsEntity::getId, current, size);
	}

	public Page<InsiderTradeEntity> pageInsiderTrade(long current, long size, String code, LocalDate start,
			LocalDate end) {
		LambdaQueryWrapper<InsiderTradeEntity> wrapper = new LambdaQueryWrapper<InsiderTradeEntity>()
			.eq(code != null && !code.isBlank(), InsiderTradeEntity::getCode, code)
			.ge(start != null, InsiderTradeEntity::getAnnDate, start)
			.le(end != null, InsiderTradeEntity::getAnnDate, end)
			.orderByDesc(InsiderTradeEntity::getAnnDate, InsiderTradeEntity::getId);
		return pageDelayed(insiderTradeMapper, wrapper, InsiderTradeEntity::getId, current, size);
	}

	public Page<FundFlowDailyEntity> pageFundFlow(long current, long size, String code, LocalDate start,
			LocalDate end) {
		LambdaQueryWrapper<FundFlowDailyEntity> wrapper = new LambdaQueryWrapper<FundFlowDailyEntity>()
			.eq(code != null && !code.isBlank(), FundFlowDailyEntity::getCode, code)
			.ge(start != null, FundFlowDailyEntity::getTradeDate, start)
			.le(end != null, FundFlowDailyEntity::getTradeDate, end)
			.orderByDesc(FundFlowDailyEntity::getTradeDate, FundFlowDailyEntity::getId);
		return pageDelayed(fundFlowDailyMapper, wrapper, FundFlowDailyEntity::getId, current, size);
	}

	public Page<DragonTigerEntity> pageDragonTiger(long current, long size, LocalDate tradeDate, String code,
			LocalDate start, LocalDate end, String reason) {
		if (tradeDate == null && (code == null || code.isBlank())) {
			throw new IllegalArgumentException("tradeDate（全市场视图）或 code 至少传一项");
		}
		LambdaQueryWrapper<DragonTigerEntity> wrapper = new LambdaQueryWrapper<DragonTigerEntity>()
			.eq(tradeDate != null, DragonTigerEntity::getTradeDate, tradeDate)
			.eq(code != null && !code.isBlank(), DragonTigerEntity::getCode, code)
			.ge(start != null, DragonTigerEntity::getTradeDate, start)
			.le(end != null, DragonTigerEntity::getTradeDate, end)
			.like(reason != null && !reason.isBlank(), DragonTigerEntity::getReason, reason)
			.orderByDesc(DragonTigerEntity::getTradeDate, DragonTigerEntity::getId);
		return pageDelayed(dragonTigerMapper, wrapper, DragonTigerEntity::getId, current, size);
	}

	public Page<RestrictedReleaseEntity> pageRestrictedRelease(long current, long size, String code,
			LocalDate start, LocalDate end) {
		LambdaQueryWrapper<RestrictedReleaseEntity> wrapper = new LambdaQueryWrapper<RestrictedReleaseEntity>()
			.eq(code != null && !code.isBlank(), RestrictedReleaseEntity::getCode, code)
			.ge(start != null, RestrictedReleaseEntity::getPlanDate, start)
			.le(end != null, RestrictedReleaseEntity::getPlanDate, end)
			.orderByDesc(RestrictedReleaseEntity::getPlanDate, RestrictedReleaseEntity::getId);
		return pageDelayed(restrictedReleaseMapper, wrapper, RestrictedReleaseEntity::getId, current, size);
	}

	public Page<IndustryBoardDailyEntity> pageBoardDaily(long current, long size, String boardName,
			LocalDate start, LocalDate end) {
		LambdaQueryWrapper<IndustryBoardDailyEntity> wrapper = new LambdaQueryWrapper<IndustryBoardDailyEntity>()
			.eq(boardName != null && !boardName.isBlank(), IndustryBoardDailyEntity::getBoardName, boardName)
			.ge(start != null, IndustryBoardDailyEntity::getTradeDate, start)
			.le(end != null, IndustryBoardDailyEntity::getTradeDate, end)
			.orderByDesc(IndustryBoardDailyEntity::getTradeDate, IndustryBoardDailyEntity::getId);
		return pageDelayed(industryBoardDailyMapper, wrapper, IndustryBoardDailyEntity::getId, current, size);
	}

	public Page<FinancialIndicatorEntity> pageFinancial(long current, long size, String code, LocalDate reportPeriod,
			String source) {
		LambdaQueryWrapper<FinancialIndicatorEntity> wrapper = new LambdaQueryWrapper<FinancialIndicatorEntity>()
			.eq(code != null && !code.isBlank(), FinancialIndicatorEntity::getCode, code)
			.eq(reportPeriod != null, FinancialIndicatorEntity::getReportPeriod, reportPeriod)
			.eq(source != null && !source.isBlank(), FinancialIndicatorEntity::getSource, source)
			.orderByDesc(FinancialIndicatorEntity::getReportPeriod, FinancialIndicatorEntity::getId);
		return pageDelayed(financialIndicatorMapper, wrapper, FinancialIndicatorEntity::getId, current, size);
	}

}
