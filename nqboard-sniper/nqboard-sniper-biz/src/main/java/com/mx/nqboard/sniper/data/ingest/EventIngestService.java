package com.mx.nqboard.sniper.data.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.CompanyNewsEntity;
import com.mx.nqboard.sniper.api.entity.DragonTigerEntity;
import com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity;
import com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity;
import com.mx.nqboard.sniper.api.entity.InsiderTradeEntity;
import com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity;
import com.mx.nqboard.sniper.api.enums.SentimentEnum;
import com.mx.nqboard.sniper.data.model.NewsItem;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.data.provider.tushare.TushareRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 事件数据入库编排（主文档 §4.3.7~§4.3.12；deep 票按需 + 日更龙虎榜，方法幂等可重跑）：
 * </p>
 * <ul>
 * <li>单位纪律：事件/资金类表<b>入库即换算</b>成 Python client 对外单位——资金流/市值 万元×1e4→元
 * （main_net 阈值按元硬编码）、解禁 float_ratio 百分点÷100→小数（同值近似写 float_mv_ratio，tushare 口径）</li>
 * <li>财务两腿各存一行（UK 含 source）：腿1 fina_indicator（§4.3.7 允许的 THS 替代源）利润率/成长
 * 百分比÷100；腿2 daily_basic 估值原样</li>
 * <li>增减持：in_de='DE' 时 change_vol 取负（减持为负口径）；ann_date 空则 demat_date 兜底再退 end_date</li>
 * <li>新闻：sentiment/announcement_type 入库时由 {@link NewsClassifier} 照 Python 规则预打标；
 * published_at 来自 date 前 10 位（当日 00:00:00）</li>
 * <li>解禁查询失败 fail-open：返回空列表不抛异常（gate 语义）</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestService {

	private static final DateTimeFormatter COMPACT = DateTimeFormatter.BASIC_ISO_DATE;

	private static final BigDecimal WAN = BigDecimal.valueOf(10_000);

	private static final int BATCH = 1000;

	private final com.mx.nqboard.sniper.data.provider.tushare.TushareClient tushareClient;

	private final CompositeProvider compositeProvider;

	private final SniperUpsertMapper upsertMapper;

	// ------------------------------------------------------------------
	// 财务指标（双腿，§4.3.7）
	// ------------------------------------------------------------------

	/** 腿1：fina_indicator 报告期行（grossprofit_margin/netprofit_margin/roe/or_yoy/netprofit_yoy，%÷100） */
	public int ingestFinancialFundamentals(String code, LocalDate start, LocalDate end) {
		String tsCode = toTsCode(code);
		List<FinancialIndicatorEntity> entities = tushareClient.finaIndicator(tsCode, start.format(COMPACT),
				end.format(COMPACT)).stream().map(row -> {
					FinancialIndicatorEntity e = new FinancialIndicatorEntity();
					IngestAudit.fill(e);
					e.setId(IngestAudit.newId());
					e.setCode(code);
					e.setReportPeriod(row.date("end_date"));
					e.setGrossMargin(toRatio(row.dec("grossprofit_margin")));
					e.setNetMargin(toRatio(row.dec("netprofit_margin")));
					e.setRoe(toRatio(row.dec("roe")));
					e.setRevenueGrowth(toRatio(row.dec("or_yoy")));
					e.setProfitGrowth(toRatio(row.dec("netprofit_yoy")));
					e.setSource("ths");
					e.setFetchedAt(LocalDateTime.now());
					return e;
				}).filter(e -> e.getReportPeriod() != null).toList();
		return batchUpsert("financial:ths:" + code, entities, upsertMapper::upsertFinancialIndicator);
	}

	/** 腿2：daily_basic 估值行（pe/pb/ps 原样；total_mv 万元×1e4→元；report_period=估值交易日） */
	public int ingestFinancialValuation(String code, LocalDate start, LocalDate end) {
		String tsCode = toTsCode(code);
		List<FinancialIndicatorEntity> entities = tushareClient.dailyBasic(tsCode, start.format(COMPACT),
				end.format(COMPACT)).stream().map(row -> {
					FinancialIndicatorEntity e = new FinancialIndicatorEntity();
					IngestAudit.fill(e);
					e.setId(IngestAudit.newId());
					e.setCode(code);
					e.setReportPeriod(row.date("trade_date"));
					e.setPeTtm(row.dec("pe_ttm"));
					e.setPb(row.dec("pb"));
					e.setPsTtm(row.dec("ps_ttm"));
					BigDecimal totalMv = row.dec("total_mv");
					e.setMarketCap(totalMv == null ? null : totalMv.multiply(WAN));
					e.setSource("tushare");
					e.setFetchedAt(LocalDateTime.now());
					return e;
				}).filter(e -> e.getReportPeriod() != null).toList();
		return batchUpsert("financial:tushare:" + code, entities, upsertMapper::upsertFinancialIndicator);
	}

	// ------------------------------------------------------------------
	// 个股新闻（§4.3.8：东财 jsonp 单源 + 入库时预打标）
	// ------------------------------------------------------------------

	public int ingestNews(String code, int limit) {
		LocalDate today = LocalDate.now();
		List<CompanyNewsEntity> entities = compositeProvider.news(code, limit).stream().map(item -> {
			CompanyNewsEntity e = new CompanyNewsEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setCode(code);
			String title = truncate(item.title(), 200);
			String body = truncate(item.content(), 2000);
			e.setTitle(title);
			e.setSourceName(item.sourceName());
			e.setUrl(item.url());
			e.setBodyDigest(body);
			LocalDate published = parseDate(item.date());
			e.setPublishedAt(published == null ? null : published.atStartOfDay());
			// 入库时预打标（sentiment 非法值不会出现——分类器只产三态）
			e.setSentiment(SentimentEnum.valueOf(NewsClassifier.sentiment(title, body).toUpperCase()));
			e.setAnnouncementType(NewsClassifier.announcementType(title, body));
			e.setFetchDate(today);
			e.setFetchedAt(LocalDateTime.now());
			return e;
		}).filter(e -> e.getPublishedAt() != null && e.getTitle() != null).toList();
		return batchUpsert("news:" + code, entities, upsertMapper::upsertCompanyNews);
	}

	// ------------------------------------------------------------------
	// 股东增减持（§4.3.9：窗口 asOf−365d~asOf；in_de=DE 取负）
	// ------------------------------------------------------------------

	public int ingestInsiderTrades(String code, LocalDate end) {
		return ingestInsiderTrades(code, end.minusDays(365), end);
	}

	public int ingestInsiderTrades(String code, LocalDate start, LocalDate end) {
		String tsCode = toTsCode(code);
		List<InsiderTradeEntity> entities = tushareClient
			.stkHoldertrade(tsCode, start.format(COMPACT), end.format(COMPACT)).stream().map(row -> {
				InsiderTradeEntity e = new InsiderTradeEntity();
				IngestAudit.fill(e);
				e.setId(IngestAudit.newId());
				e.setCode(code);
				// ann_date 空则 demat_date 兜底，再退请求 end_date
				LocalDate ann = row.date("ann_date");
				if (ann == null) {
					ann = row.date("demat_date");
				}
				if (ann == null) {
					ann = end;
				}
				e.setAnnDate(ann);
				e.setHolderName(row.str("holder_name"));
				e.setHolderType(row.str("holder_type"));
				BigDecimal vol = row.dec("change_vol");
				if (vol != null && "DE".equalsIgnoreCase(row.str("in_de"))) {
					vol = vol.abs().negate();
				}
				e.setChangeVol(vol);
				e.setAvgPrice(row.dec("avg_price"));
				e.setAfterShares(row.dec("after_share"));
				e.setSource("tushare");
				e.setFetchedAt(LocalDateTime.now());
				return e;
			}).toList();
		return batchUpsert("insider:" + code, entities, upsertMapper::upsertInsiderTrade);
	}

	// ------------------------------------------------------------------
	// 主力资金流（§4.3.10：窗口 asOf−30d~asOf；万元×1e4→元，main_net 必须为元）
	// ------------------------------------------------------------------

	public int ingestFundFlow(String code, LocalDate end) {
		String tsCode = toTsCode(code);
		List<FundFlowDailyEntity> entities = tushareClient
			.moneyflow(tsCode, end.minusDays(30).format(COMPACT), end.format(COMPACT)).stream()
			.map(row -> {
				FundFlowDailyEntity e = new FundFlowDailyEntity();
				IngestAudit.fill(e);
				e.setId(IngestAudit.newId());
				e.setCode(code);
				e.setTradeDate(row.date("trade_date"));
				e.setMainNet(wanToYuan(row.dec("net_mf_amount")));
				e.setSuperNet(wanDiff(row.dec("buy_elg_amount"), row.dec("sell_elg_amount")));
				e.setLargeNet(wanDiff(row.dec("buy_lg_amount"), row.dec("sell_lg_amount")));
				e.setMediumNet(wanDiff(row.dec("buy_md_amount"), row.dec("sell_md_amount")));
				e.setSmallNet(wanDiff(row.dec("buy_sm_amount"), row.dec("sell_sm_amount")));
				e.setSource("tushare");
				e.setFetchedAt(LocalDateTime.now());
				return e;
			}).filter(e -> e.getTradeDate() != null).toList();
		return batchUpsert("fund_flow:" + code, entities, upsertMapper::upsertFundFlowDaily);
	}

	// ------------------------------------------------------------------
	// 龙虎榜（§4.3.11：top_list 按日全市场 1 调，金额元原样）
	// ------------------------------------------------------------------

	public int ingestDragonTiger(LocalDate tradeDate) {
		List<DragonTigerEntity> entities = tushareClient.topList(tradeDate.format(COMPACT)).stream().map(row -> {
			DragonTigerEntity e = new DragonTigerEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setCode(toCode(row.str("ts_code")));
			e.setTradeDate(row.date("trade_date"));
			e.setReason(row.str("reason"));
			e.setNetBuy(row.dec("net_amount"));
			e.setBuyAmt(row.dec("l_buy"));
			e.setSellAmt(row.dec("l_sell"));
			e.setChangePct(row.dec("pct_change"));
			e.setSource("tushare");
			e.setFetchedAt(LocalDateTime.now());
			return e;
		}).filter(e -> e.getTradeDate() != null && e.getCode() != null).toList();
		return batchUpsert("dragon_tiger:" + tradeDate, entities, upsertMapper::upsertDragonTiger);
	}

	// ------------------------------------------------------------------
	// 限售解禁（§4.3.12：窗口 −3y~+2y；float_ratio 百分点÷100、同值近似 float_mv_ratio；
	// 查询失败 fail-open 返回空）
	// ------------------------------------------------------------------

	public int ingestRestrictedRelease(String code, LocalDate asOf) {
		String tsCode = toTsCode(code);
		List<RestrictedReleaseEntity> entities;
		try {
			entities = tushareClient
				.shareFloat(tsCode, asOf.minusYears(3).format(COMPACT), asOf.plusYears(2).format(COMPACT))
				.stream()
				.map(row -> {
					RestrictedReleaseEntity e = new RestrictedReleaseEntity();
					IngestAudit.fill(e);
					e.setId(IngestAudit.newId());
					e.setCode(code);
					e.setPlanDate(row.date("float_date"));
					e.setShares(row.dec("float_share"));
					// 百分点→小数（8.0→0.08）；tushare 口径实为占总股本，同值近似写 float_mv_ratio（更小更保守）
					BigDecimal ratio = toRatio(row.dec("float_ratio"));
					e.setFloatRatio(ratio);
					e.setFloatMvRatio(ratio);
					e.setSource("tushare");
					e.setFetchedAt(LocalDateTime.now());
					return e;
				})
				.filter(e -> e.getPlanDate() != null)
				.toList();
		}
		catch (RuntimeException e) {
			// fail-open（gate 解禁否决语义）：查询失败不拦截信号，缺数标记由调用方记录
			log.warn("restricted_release failed for {} (fail-open): {}", code, e.getMessage());
			return 0;
		}
		return batchUpsert("restricted:" + code, entities, upsertMapper::upsertRestrictedRelease);
	}

	// ------------------------------------------------------------------
	// 私有段
	// ------------------------------------------------------------------

	private static BigDecimal toRatio(BigDecimal percent) {
		// 百分点→小数（91.19→0.9119），scale 6 覆盖 decimal(10,4) 精度
		return percent == null ? null : percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
	}

	private static BigDecimal wanToYuan(BigDecimal wan) {
		return wan == null ? null : wan.multiply(WAN);
	}

	private static BigDecimal wanDiff(BigDecimal buyWan, BigDecimal sellWan) {
		if (buyWan == null || sellWan == null) {
			return null;
		}
		return buyWan.subtract(sellWan).multiply(WAN);
	}

	private static String toTsCode(String code) {
		if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
			return code + ".SH";
		}
		if (code.startsWith("4") || code.startsWith("8")) {
			return code + ".BJ";
		}
		return code + ".SZ";
	}

	private static String toCode(String tsCode) {
		if (tsCode == null) {
			return null;
		}
		int dot = tsCode.indexOf('.');
		return dot > 0 ? tsCode.substring(0, dot) : tsCode;
	}

	private static LocalDate parseDate(String date) {
		if (date == null || date.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(date.substring(0, 10));
		}
		catch (java.time.format.DateTimeParseException e) {
			return null;
		}
	}

	private static String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max);
	}

	private <E> int batchUpsert(String label, List<E> entities, java.util.function.ToIntFunction<List<E>> upsert) {
		if (entities.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (int i = 0; i < entities.size(); i += BATCH) {
			total += upsert.applyAsInt(entities.subList(i, Math.min(i + BATCH, entities.size())));
		}
		log.info("ingest {}: {} rows (upsert {})", label, entities.size(), total);
		return total;
	}

}
