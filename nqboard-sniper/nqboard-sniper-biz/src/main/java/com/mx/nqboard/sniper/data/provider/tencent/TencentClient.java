package com.mx.nqboard.sniper.data.provider.tencent;

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
 * 腾讯行情兜底客户端（照 Python web_fallback.get_kline_tencent / get_realtime_quotes_tencent）：
 * </p>
 * <ul>
 * <li>fqkline：param={sh|sz}{code},day,{YYYY-MM-DD},{YYYY-MM-DD},640[,qfq|hfq]；
 * 行格式 [日期,开盘,收盘,最高,最低,成交量(手),…]（<b>O-C-H-L</b>）；单次上限 640 根——
 * Python 现状超窗即截断，Java 照主文档分段拉（行为超集，对拍选短窗口不受影响）</li>
 * <li>批量报价：qt.gtimg.cn/q=（<b>GBK 解码</b>，60 码/批，maxAttempts=2），
 * payload 按 ~ 分割取位置：2=代码 1=名称 3=最新 4=昨收 5=今开 32=涨跌幅 33/34=最高/最低
 * 36=总手(手) 37=成交额(<b>万元×1e4→元</b>) 38=换手率</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class TencentClient {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Charset GBK = Charset.forName("GBK");

	private static final int BARS_PER_REQUEST = 640;

	private static final int QUOTES_PER_BATCH = 60;

	public static final String FQKLINE_URL = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get";

	public static final String QUOTES_URL_PREFIX = "https://qt.gtimg.cn/q=";

	private final ThrottledHttpClient http;

	private final String fqklineUrl;

	private final String quotesUrlPrefix;

	public TencentClient(RequestAuditSink auditSink) {
		this(auditSink, FQKLINE_URL, QUOTES_URL_PREFIX);
	}

	/** 包内测试构造：URL 可覆盖为内嵌 server */
	public TencentClient(RequestAuditSink auditSink, String fqklineUrl, String quotesUrlPrefix) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("User-Agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
						+ "Chrome/124.0.0.0 Safari/537.36");
		// Python 版 Connection: close 由 JDK 受限头过滤 + stale 重试语义兜底（见 ThrottledHttpClient）
		this.http = new ThrottledHttpClient("tencent", ThrottledHttpClient.DEFAULT_MIN_DELAY,
				ThrottledHttpClient.DEFAULT_BACKOFF_SECONDS, ThrottledHttpClient.DEFAULT_HTTP_TIMEOUT, null,
				auditSink, headers);
		this.fqklineUrl = fqklineUrl;
		this.quotesUrlPrefix = quotesUrlPrefix;
	}

	/**
	 * 日 K 线（adjust：none/qfq/hfq；fq 为空时 key=day，否则 {fq}day）。
	 */
	public List<KlineBar> fqkline(String code, String exchange, LocalDate start, LocalDate end, String adjust) {
		String symbol = exchange.toLowerCase() + code;
		String fq = "qfq".equals(adjust) || "hfq".equals(adjust) ? adjust : "";
		String key = fq.isEmpty() ? "day" : fq + "day";
		List<KlineBar> bars = new ArrayList<>();
		LocalDate cursor = start;
		while (!cursor.isAfter(end)) {
			String param = symbol + ",day," + cursor + "," + end + "," + BARS_PER_REQUEST
					+ (fq.isEmpty() ? "" : "," + fq);
			Map<String, String> params = new LinkedHashMap<>();
			params.put("param", param);
			String payload;
			try {
				payload = http.get(fqklineUrl, params, null, 3);
			}
			catch (RuntimeException e) {
				log.warn("tencent kline failed for {}: {}", symbol, e.getMessage());
				return List.of();
			}
			JsonNode rows;
			try {
				// 同一 payload 内先查 {fq}day 再回退 day（照 Python payload.get(key) or payload.get("day")，不二次发请求）
				JsonNode node = MAPPER.readTree(payload).path("data").path(symbol);
				JsonNode keyed = node.path(key);
				rows = keyed.isArray() && !keyed.isEmpty() ? keyed : node.path("day");
			}
			catch (java.io.IOException e) {
				log.warn("tencent kline bad json for {}: {}", symbol, e.getMessage());
				return List.of();
			}
			if (!rows.isArray() || rows.isEmpty()) {
				break;
			}
			LocalDate last = cursor;
			for (JsonNode row : rows) {
				if (!row.isArray() || row.size() < 6) {
					continue;
				}
				LocalDate date = LocalDate.parse(row.get(0).asText().substring(0, 10));
				if (date.isBefore(start) || date.isAfter(end)) {
					continue;
				}
				// 行格式 [date, open, close, high, low, vol(手), ...] —— O-C-H-L
				bars.add(new KlineBar(code, date, dec(row.get(1)), dec(row.get(3)), dec(row.get(4)),
						dec(row.get(2)), dec(row.get(5)), null, null, "tencent"));
				last = date;
			}
			if (bars.isEmpty() || rows.size() < BARS_PER_REQUEST || !last.isAfter(cursor.minusDays(1))) {
				break;
			}
			cursor = last.plusDays(1);
		}
		return bars;
	}

	/**
	 * 批量实时报价（GBK，60 码/批；成交额万元→元）。
	 */
	public List<QuoteSnapshot> batchQuotes(List<String> codes) {
		List<QuoteSnapshot> result = new ArrayList<>();
		for (int i = 0; i < codes.size(); i += QUOTES_PER_BATCH) {
			List<String> batch = codes.subList(i, Math.min(i + QUOTES_PER_BATCH, codes.size()));
			String query = String.join(",", batch);
			String body;
			try {
				body = http.get(quotesUrlPrefix + query, Map.of(), GBK, 2);
			}
			catch (RuntimeException e) {
				log.warn("tencent quotes failed: {}", e.getMessage());
				continue;
			}
			for (String line : body.strip().split(";")) {
				line = line.strip();
				int eq = line.indexOf('=');
				if (eq < 0 || !line.contains("~")) {
					continue;
				}
				String[] parts = line.substring(eq + 1).strip().replace("\"", "").split("~");
				if (parts.length < 38) {
					continue;
				}
				// 位置：1=名称 2=代码 3=最新 4=昨收 5=今开 32=涨跌幅 33=最高 34=最低 36=总手 37=成交额(万元) 38=换手率
				String code = parts[2].strip();
				// zfill(6) 语义：不足 6 位左补零
				String normalized = code.length() >= 6 ? code : "0".repeat(6 - code.length()) + code;
				BigDecimal amount = dec(parts[37]);
				result.add(new QuoteSnapshot(normalized, parts[1], dec(parts[3]), dec(parts[32]), dec(parts[5]),
						dec(parts[33]), dec(parts[34]), dec(parts[4]), dec(parts[36]),
						amount == null ? null : amount.multiply(BigDecimal.valueOf(10_000)), dec(parts[38]),
						"tencent"));
			}
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
