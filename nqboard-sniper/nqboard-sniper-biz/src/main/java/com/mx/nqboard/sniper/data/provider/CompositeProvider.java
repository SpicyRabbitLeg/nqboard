package com.mx.nqboard.sniper.data.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.NewsItem;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.mx.nqboard.sniper.data.provider.em.EastmoneyClient;
import com.mx.nqboard.sniper.data.provider.sina.SinaClient;
import com.mx.nqboard.sniper.data.provider.support.CircuitBreaker;
import com.mx.nqboard.sniper.data.provider.support.ThrottledHttpClient;
import com.mx.nqboard.sniper.data.provider.tencent.TencentClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareRow;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 数据源路由层（照 Python providers/composite.py 路由语义）——唯一的外部 HTTP 接入点，
 * 上层（T8/T9 入库服务、T10 DataGateway）只认本类：
 * </p>
 * <ul>
 * <li>行情 4 级链：Tushare → 东财K线 → 腾讯fqkline → 新浪（<b>qfq 请求不降级新浪</b>，宁可缺数淘汰）；
 * Tushare 级 qfq 用"窗口因子 + maxFactorSupplier（库内全历史 max(factor)）"本地合成，缺依赖时跳过 Tushare 级</li>
 * <li>反向兜底：非 Tushare 优先模式下三级全空按 CN_PRICE_FALLBACK（默认 tushare）回退（composite.py:105-134）</li>
 * <li>快照链：预算<b>无条件消耗 1 次</b>（B.1 #1）→ 东财 clist → 新浪行情中心 → 腾讯批量（代码表由调用方从库读传入）</li>
 * <li>事件端点：Tushare 优先、空/失败降东财兜底（`_routed` 语义）；f127 行业 EM 直连优先（与板块名对齐）</li>
 * <li>预算：仅"东财主源"模式（非 tusharePreferred）对东财请求计数；快照无条件计 1；
 * 耗尽行为（Stage1 跳票）属管线层</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class CompositeProvider {

	private final TushareClient tushare;

	private final EastmoneyClient eastmoney;

	private final TencentClient tencent;

	private final SinaClient sina;

	private final DailyBudget dailyBudget;

	/** 行情 Tushare 优先（照 tushare_preferred_for_prices：CN_DATA_PROVIDER=tushare 且 token 可用） */
	private final boolean tusharePreferredForPrices;

	/** 非行情端点 Tushare 优先（照 CN_TUSHARE_FIRST） */
	private final boolean tushareFirst;

	/** 非优先模式行情三级全空时回退 Tushare（照 CN_PRICE_FALLBACK=tushare） */
	private final boolean priceFallbackTushare;

	public CompositeProvider(TushareClient tushare, EastmoneyClient eastmoney, TencentClient tencent,
			SinaClient sina, DailyBudget dailyBudget) {
		this(tushare, eastmoney, tencent, sina, dailyBudget, true, true, true);
	}

	public CompositeProvider(TushareClient tushare, EastmoneyClient eastmoney, TencentClient tencent,
			SinaClient sina, DailyBudget dailyBudget, boolean tusharePreferredForPrices, boolean tushareFirst,
			boolean priceFallbackTushare) {
		this.tushare = tushare;
		this.eastmoney = eastmoney;
		this.tencent = tencent;
		this.sina = sina;
		this.dailyBudget = dailyBudget;
		this.tusharePreferredForPrices = tusharePreferredForPrices;
		this.tushareFirst = tushareFirst;
		this.priceFallbackTushare = priceFallbackTushare;
	}

	/** 逐票缺口补拉的 Tushare 物料：none 行 + 窗口复权因子（qfq 合成在入库层编排） */
	public record TushareDailyBundle(List<KlineBar> noneBars, List<BigDecimal> factors) {
	}

	/**
	 * 行情 4 级降级链（逐票缺口补拉；日更全市场批量不走本方法，直接用 TushareClient 批量）。
	 * @param code 6 位代码
	 * @param exchange SH/SZ/BJ
	 * @param adjust 请求口径（none/qfq）
	 * @param maxFactorSupplier 该票全历史 max(factor)（库内查询，供 Tushare 级 qfq 合成；null 时 Tushare 级跳过 qfq）
	 */
	public List<KlineBar> prices(String code, String exchange, LocalDate start, LocalDate end, AdjustEnum adjust,
			Function<String, BigDecimal> maxFactorSupplier) {
		String startCompact = toCompact(start);
		String endCompact = toCompact(end);
		boolean qfq = adjust == AdjustEnum.QFQ;

		// 1. Tushare 优先（qfq 需合成依赖，缺则跳过该级——窗口内因子算不出全历史 max）
		if (tusharePreferredForPrices && (tushare != null)) {
			if (!qfq || maxFactorSupplier != null) {
				try {
					List<KlineBar> bars = tushareLevel(code, startCompact, endCompact, adjust, maxFactorSupplier);
					if (!bars.isEmpty()) {
						return bars;
					}
					log.debug("tushare prices empty for {}, falling back to web sources", code);
				}
				catch (RuntimeException e) {
					log.warn("tushare prices failed for {} ({}); using web fallback", code, e.getMessage());
				}
			}
		}

		// 2. 东财 K线（fqt: 0=none 1=qfq）
		if (tusharePreferredForPrices || consumeBudget("prices:" + code)) {
			try {
				List<KlineBar> bars = eastmoney.stockKline(code, qfq ? "1" : "0", startCompact, endCompact);
				if (!bars.isEmpty()) {
					return withCode(bars, code);
				}
			}
			catch (RuntimeException e) {
				log.warn("em kline failed for {} ({}); falling back to tencent", code, e.getMessage());
			}
		}

		// 3. 腾讯 fqkline
		if (tusharePreferredForPrices || consumeBudget("tencent-prices:" + code)) {
			try {
				List<KlineBar> bars = tencent.fqkline(code, exchange, start, end, qfq ? "qfq" : "none");
				if (!bars.isEmpty()) {
					return bars;
				}
			}
			catch (RuntimeException e) {
				log.warn("tencent kline failed for {} ({}); falling back to sina", code, e.getMessage());
			}
		}

		// 4. 新浪 K线——仅不复权可用；qfq 请求宁可缺数淘汰，绝不让不复权数据污染复权口径
		if (!qfq) {
			if (tusharePreferredForPrices || consumeBudget("sina-prices:" + code)) {
				try {
					List<KlineBar> bars = sina.kline(code, exchange, start, end);
					if (!bars.isEmpty()) {
						return bars;
					}
				}
				catch (RuntimeException e) {
					log.warn("sina kline failed for {} ({}); price chain exhausted", code, e.getMessage());
				}
			}
		}
		else {
			log.info("prices(qfq) for {} not served by web sources (sina unadjusted disallowed)", code);
		}

		// 反向兜底：非 Tushare 优先模式三级全空，按 CN_PRICE_FALLBACK 回退 Tushare
		if (!tusharePreferredForPrices && priceFallbackTushare && tushare != null) {
			try {
				List<KlineBar> bars = tushareLevel(code, startCompact, endCompact, adjust, maxFactorSupplier);
				if (!bars.isEmpty()) {
					log.info("prices for {} served by tushare price-fallback", code);
					return bars;
				}
			}
			catch (RuntimeException e) {
				log.warn("tushare price-fallback failed for {}: {}", code, e.getMessage());
			}
		}
		return List.of();
	}

	private List<KlineBar> tushareLevel(String code, String startCompact, String endCompact, AdjustEnum adjust,
			Function<String, BigDecimal> maxFactorSupplier) {
		String tsCode = toTsCode(code);
		List<TushareRow> dailyRows = tushare.dailyByCode(tsCode, startCompact, endCompact);
		if (dailyRows.isEmpty()) {
			return List.of();
		}
		if (adjust == AdjustEnum.NONE) {
			return dailyRows.stream().map(row -> toBar(code, row)).toList();
		}
		// qfq = none × factor ÷ max_factor（库内全历史最大因子；窗口因子随行取）
		List<TushareRow> factorRows = tushare.adjFactorByCode(tsCode, startCompact, endCompact);
		return synthQfq(code, dailyRows, factorRows, maxFactorSupplier);
	}

	/** qfq 本地合成（pro_bar 等价语义：OHLC 四列同乘 factor÷max_factor，volume/amount/pre_close 不乘） */
	public static List<KlineBar> synthQfq(String code, List<TushareRow> dailyRows, List<TushareRow> factorRows,
			Function<String, BigDecimal> maxFactorSupplier) {
		if (maxFactorSupplier == null) {
			return List.of();
		}
		BigDecimal maxFactor = maxFactorSupplier.apply(code);
		if (maxFactor == null || maxFactor.signum() <= 0) {
			return List.of();
		}
		java.util.Map<LocalDate, BigDecimal> factorByDate = new java.util.LinkedHashMap<>();
		for (TushareRow row : factorRows) {
			LocalDate date = row.date("trade_date");
			BigDecimal factor = row.dec("adj_factor");
			if (date != null && factor != null) {
				factorByDate.put(date, factor);
			}
		}
		List<KlineBar> bars = new ArrayList<>();
		for (TushareRow row : dailyRows) {
			LocalDate date = row.date("trade_date");
			BigDecimal factor = factorByDate.get(date);
			if (date == null || factor == null) {
				continue;
			}
			// OHLC 四列同乘；volume/amount/pre_close 不乘（复权只作用价格）
			bars.add(new KlineBar(code, date, scale(row.dec("open").multiply(factor).divide(maxFactor, 10,
					java.math.RoundingMode.HALF_UP)), scale(row.dec("high").multiply(factor)
						.divide(maxFactor, 10, java.math.RoundingMode.HALF_UP)),
					scale(row.dec("low").multiply(factor).divide(maxFactor, 10, java.math.RoundingMode.HALF_UP)),
					scale(row.dec("close").multiply(factor).divide(maxFactor, 10, java.math.RoundingMode.HALF_UP)),
					row.dec("vol"), row.dec("amount"), row.dec("pre_close"), "tushare"));
		}
		return bars;
	}

	private static BigDecimal scale(BigDecimal value) {
		return value == null ? null : value;
	}

	/**
	 * 全市场快照 3 级链：东财 clist → 新浪行情中心 → 腾讯批量（代码表由调用方从 sniper_stock_basic 读传入，
	 * 格式 sh600519/sz000001；B.1 #4：无代码表时腾讯级为空）。预算<b>无条件消耗 1 次</b>（B.1 #1）。
	 */
	public List<QuoteSnapshot> spot(List<String> tencentSymbols) {
		if (!consumeBudget("spot")) {
			log.warn("daily budget exhausted before spot fetch");
			return List.of();
		}
		try {
			List<QuoteSnapshot> snapshots = eastmoney.spotAll();
			if (!snapshots.isEmpty()) {
				return snapshots;
			}
		}
		catch (RuntimeException e) {
			log.warn("em spot failed ({}); falling back to sina market-center", e.getMessage());
		}
		try {
			List<QuoteSnapshot> snapshots = sina.marketSpot(null);
			if (!snapshots.isEmpty()) {
				log.info("sina market-center spot served {} rows", snapshots.size());
				return snapshots;
			}
		}
		catch (RuntimeException e) {
			log.warn("sina market-center spot failed ({}); falling back to tencent batch", e.getMessage());
		}
		if (tencentSymbols != null && !tencentSymbols.isEmpty()) {
			try {
				List<QuoteSnapshot> snapshots = tencent.batchQuotes(tencentSymbols);
				if (!snapshots.isEmpty()) {
					log.info("tencent batch spot served {} rows", snapshots.size());
					return snapshots;
				}
			}
			catch (RuntimeException e) {
				log.warn("tencent batch spot failed: {}", e.getMessage());
			}
		}
		log.error("all spot sources failed (em/sina/tencent)");
		return List.of();
	}

	/**
	 * 个股新闻（Tushare 无等价端点，东财 jsonp 单源——主源与兜底同源，附录 A #20）。
	 */
	public List<NewsItem> news(String code, int limit) {
		return eastmoney.searchNews(code, limit);
	}

	/**
	 * 行业板块日K（东财板块K线单源；板块名→BK 码解析在 EM client 内，缓存归上层网关）。
	 */
	public List<KlineBar> boardKline(String boardName, LocalDate start, LocalDate end) {
		return eastmoney.boardKline(boardName, start.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
				end.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
	}

	/**
	 * 行业分类：f127 EM 直连优先（与板块名对齐），失败降 Tushare stock_basic 旧口径兜底。
	 */
	public String industry(String code, Function<String, String> tushareIndustryFallback) {
		try {
			String industry = eastmoney.industryByCode(code);
			if (industry != null) {
				return industry;
			}
		}
		catch (RuntimeException e) {
			log.warn("em industry failed for {}: {}", code, e.getMessage());
		}
		return tushareIndustryFallback != null ? tushareIndustryFallback.apply(code) : null;
	}

	private boolean consumeBudget(String endpoint) {
		if (tusharePreferredForPrices) {
			// Tushare 主源模式：东财请求不消耗预算（tushare_preferred_for_prices 语义）
			return true;
		}
		return dailyBudget.tryConsume(1);
	}

	private static KlineBar toBar(String code, TushareRow row) {
		return new KlineBar(code, row.date("trade_date"), row.dec("open"), row.dec("high"), row.dec("low"),
				row.dec("close"), row.dec("vol"), row.dec("amount"), row.dec("pre_close"), "tushare");
	}

	/** EM 兜底行补写 code（EM K线行 code 语义由调用方按请求传入） */
	private static List<KlineBar> withCode(List<KlineBar> bars, String code) {
		List<KlineBar> out = new ArrayList<>(bars.size());
		for (KlineBar bar : bars) {
			if (bar.code() == null || bar.code().isEmpty()) {
				out.add(new KlineBar(code, bar.tradeDate(), bar.open(), bar.high(), bar.low(), bar.close(),
						bar.volume(), bar.amount(), bar.preClose(), bar.source()));
			}
			else {
				out.add(bar);
			}
		}
		return out;
	}

	private static String toCompact(LocalDate date) {
		return date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
	}

	private static String toTsCode(String code) {
		// exchange 后缀由代码段判定：6/9/5 开头 SH，4/8 开头 BJ，其余 SZ
		if (code.startsWith("6") || code.startsWith("9") || code.startsWith("5")) {
			return code + ".SH";
		}
		if (code.startsWith("4") || code.startsWith("8")) {
			return code + ".BJ";
		}
		return code + ".SZ";
	}

}
