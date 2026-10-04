package com.mx.nqboard.sniper.data.provider.sina;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.data.RequestAuditSink;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.mx.nqboard.sniper.data.provider.support.ThrottledHttpClient;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 新浪行情兜底客户端（照 Python web_fallback.get_kline_sina / get_realtime_quotes_sina /
 * get_market_spot_sina，全部带 UA + Referer: https://finance.sina.com.cn）：
 * </p>
 * <ul>
 * <li>K线：getKLineData scale=240 datalen=1023，<b>仅不复权</b>（qfq 请求不许降级到此源）；
 * volume 单位是<b>股</b>，client 层 ÷100 → 手</li>
 * <li>批量报价：hq.sinajs.cn/list=（GBK，40 码/批，maxAttempts=2），CSV 位置
 * 0=名称 1=今开 2=昨收 3=最新 4=最高 5=最低 8=成交量(股) 9=成交额(元)；涨跌幅现算</li>
 * <li>行情中心：Market_Center.getHQNodeData（num=80 分页，空页或不足一页终止；
 * trade/settlement 非空时涨跌幅优先现算；换手率无，null）</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class SinaClient {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Charset GBK = Charset.forName("GBK");

	private static final int QUOTES_PER_BATCH = 40;

	private static final int SPOT_PAGE_SIZE = 80;

	public static final String KLINE_URL = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData";

	public static final String QUOTES_URL_PREFIX = "https://hq.sinajs.cn/list=";

	public static final String SPOT_URL = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/"
			+ "Market_Center.getHQNodeData";

	private final ThrottledHttpClient http;

	private final String klineUrl;

	private final String quotesUrlPrefix;

	private final String spotUrl;

	public SinaClient(RequestAuditSink auditSink) {
		this(auditSink, KLINE_URL, QUOTES_URL_PREFIX, SPOT_URL);
	}

	/** 包内测试构造：URL 可覆盖为内嵌 server */
	public SinaClient(RequestAuditSink auditSink, String klineUrl, String quotesUrlPrefix, String spotUrl) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("User-Agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
						+ "Chrome/124.0.0.0 Safari/537.36");
		headers.put("Referer", "https://finance.sina.com.cn");
		// Python 版 Connection: close 由 JDK 受限头过滤 + stale 重试语义兜底（见 ThrottledHttpClient）
		this.http = new ThrottledHttpClient("sina", ThrottledHttpClient.DEFAULT_MIN_DELAY,
				ThrottledHttpClient.DEFAULT_BACKOFF_SECONDS, ThrottledHttpClient.DEFAULT_HTTP_TIMEOUT, null,
				auditSink, headers);
		this.klineUrl = klineUrl;
		this.quotesUrlPrefix = quotesUrlPrefix;
		this.spotUrl = spotUrl;
	}

	/**
	 * 不复权日 K 线（volume 股→手；窗口过滤照 Python）。
	 */
	public List<KlineBar> kline(String code, String exchange, LocalDate start, LocalDate end) {
		String symbol = exchange.toLowerCase() + code;
		Map<String, String> params = new LinkedHashMap<>();
		params.put("symbol", symbol);
		params.put("scale", "240");
		params.put("ma", "no");
		params.put("datalen", "1023");
		JsonNode rows;
		try {
			rows = MAPPER.readTree(http.get(klineUrl, params, null, 3));
		}
		catch (java.io.IOException | RuntimeException e) {
			log.warn("sina kline failed for {}: {}", symbol, e.getMessage());
			return List.of();
		}
		List<KlineBar> bars = new ArrayList<>();
		if (!rows.isArray()) {
			return bars;
		}
		for (JsonNode row : rows) {
			String day = row.path("day").asText("");
			if (day.length() < 10) {
				continue;
			}
			LocalDate date = LocalDate.parse(day.substring(0, 10));
			if (date.isBefore(start) || date.isAfter(end)) {
				continue;
			}
			// volume 单位股 → ÷100 手（入库前完成，与 Python web_fallback 一致）
			BigDecimal sharesVolume = dec(row.get("volume"));
			BigDecimal handsVolume = sharesVolume == null ? null : sharesVolume.divide(BigDecimal.valueOf(100));
			bars.add(new KlineBar(code, date, dec(row.get("open")), dec(row.get("high")), dec(row.get("low")),
					dec(row.get("close")), handsVolume, null, null, "sina"));
		}
		return bars;
	}

	/**
	 * 批量实时报价（hq.sinajs，GBK，40 码/批；涨跌幅由 price/prev_close 现算）。
	 */
	public List<QuoteSnapshot> batchQuotes(List<String> codes) {
		List<QuoteSnapshot> result = new ArrayList<>();
		for (int i = 0; i < codes.size(); i += QUOTES_PER_BATCH) {
			List<String> batch = codes.subList(i, Math.min(i + QUOTES_PER_BATCH, codes.size()));
			String body;
			try {
				body = http.get(quotesUrlPrefix + String.join(",", batch), Map.of(), GBK, 2);
			}
			catch (RuntimeException e) {
				log.warn("sina quotes failed: {}", e.getMessage());
				continue;
			}
			for (String line : body.strip().split("\n")) {
				int eq = line.indexOf('=');
				if (eq < 0) {
					continue;
				}
				String symbol = line.substring(0, eq).strip();
				int under = symbol.lastIndexOf('_');
				if (under < 0 || symbol.length() - under - 1 < 8) {
					continue;
				}
				// var hq_str_sh600519="名称,今开,昨收,最新,最高,最低,…"; → code 取 symbol 后 6 位
				String code = symbol.substring(symbol.length() - 6);
				String[] fields = line.substring(eq + 1).replace("\"", "").replace(";", "").split(",");
				if (fields.length < 31 || fields[0].isEmpty()) {
					continue;
				}
				BigDecimal price = dec(fields[3]);
				BigDecimal prev = dec(fields[2]);
				// 先 ×100 再除，避免中途按 scale 截断丢失有效位（对齐 Python float 语义）
				BigDecimal changePct = price != null && prev != null && prev.signum() != 0
						? price.subtract(prev).multiply(BigDecimal.valueOf(100))
							.divide(prev, 10, java.math.RoundingMode.HALF_UP)
						: null;
				BigDecimal sharesVolume = dec(fields[8]);
				BigDecimal handsVolume = sharesVolume == null ? null : sharesVolume.divide(BigDecimal.valueOf(100));
				result.add(new QuoteSnapshot(code, fields[0], price, changePct, dec(fields[1]), dec(fields[4]),
						dec(fields[5]), prev, handsVolume, dec(fields[9]), null, "sina"));
			}
		}
		return result;
	}

	/**
	 * 全市场快照（行情中心 hs_a 分页；maxPages=null 拉完全市场，~70 页）。
	 */
	public List<QuoteSnapshot> marketSpot(Integer maxPages) {
		List<QuoteSnapshot> result = new ArrayList<>();
		int page = 1;
		while (maxPages == null || page <= maxPages) {
			Map<String, String> params = new LinkedHashMap<>();
			params.put("page", String.valueOf(page));
			params.put("num", String.valueOf(SPOT_PAGE_SIZE));
			params.put("sort", "amount");
			params.put("asc", "0");
			params.put("node", "hs_a");
			params.put("symbol", "");
			params.put("_s_r_a", "page");
			JsonNode data;
			try {
				data = MAPPER.readTree(http.get(spotUrl, params, null, 2));
			}
			catch (java.io.IOException | RuntimeException e) {
				log.warn("sina market spot page {} failed: {}", page, e.getMessage());
				break;
			}
			if (!data.isArray() || data.isEmpty()) {
				break;
			}
			for (JsonNode item : data) {
				BigDecimal price = dec(item.get("trade"));
				BigDecimal prev = dec(item.get("settlement"));
				// trade/settlement 非空时现算优先；先 ×100 再除防截断
				BigDecimal changePct = price != null && prev != null && prev.signum() != 0
						? price.subtract(prev).multiply(BigDecimal.valueOf(100))
							.divide(prev, 10, java.math.RoundingMode.HALF_UP)
						: dec(item.get("changepercent"));
				String code = item.path("code").asText("");
				code = code.length() >= 6 ? code : "0".repeat(6 - code.length()) + code;
				// volume 单位股 → ÷100 手；换手率无（null）
				BigDecimal sharesVolume = dec(item.get("volume"));
				BigDecimal handsVolume = sharesVolume == null ? null : sharesVolume.divide(BigDecimal.valueOf(100));
				result.add(new QuoteSnapshot(code, item.path("name").asText(""), price, changePct,
						dec(item.get("open")), dec(item.get("high")), dec(item.get("low")), prev, handsVolume,
						dec(item.get("amount")), null, "sina"));
			}
			if (data.size() < SPOT_PAGE_SIZE) {
				break;
			}
			page++;
		}
		return result;
	}

	private BigDecimal dec(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		return dec(node.asText());
	}

	private BigDecimal dec(String text) {
		if (text == null || text.isEmpty() || "-".equals(text) || "--".equals(text)) {
			return null;
		}
		try {
			return new BigDecimal(text.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
