package com.mx.nqboard.sniper.data.provider.tushare;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.data.RequestAuditSink;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Tushare Pro HTTP 客户端（行为对照 Python {@code src/market/cn/tushare_client.py}，逐语义照搬）：
 * </p>
 * <ul>
 * <li>节流：全局锁内按"请求开始时刻"锚定最小间隔（默认 350ms，{@code _throttle} 语义）</li>
 * <li>重试：共 maxRetries 次总尝试（{@code for attempt in range(MAX_RETRIES)}），
 * 限频退避 {@code BACKOFF*(n)}、其他 {@code BACKOFF*(n)*0.5}，均加 rand(0,1) 秒（{@code _call} 语义）</li>
 * <li>限频判定：msg 含 最多访问/每分钟/频次/超过 四关键词之一（{@code _is_rate_limit_error}）</li>
 * <li>翻页：信封 {@code has_more=true} 时带 {@code offset=已取行数} 重调（SDK 内部行为在 Java 侧显式化）</li>
 * <li>审计：每次逻辑调用（含重试与翻页合并）成功/失败各回调一次 {@link RequestAuditSink}（log_request 粒度）</li>
 * </ul>
 * <p>纯 Java 类零 Spring 依赖，Spring 装配与配置接入在编排层完成。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class TushareClient {

	/** 限频关键词（照 Python _is_rate_limit_error：最多访问/每分钟/频次/超过） */
	private static final String[] RATE_LIMIT_KEYWORDS = { "最多访问", "每分钟", "频次", "超过" };

	public static final URI DEFAULT_BASE_URI = URI.create("https://api.tushare.pro");

	public static final Duration DEFAULT_THROTTLE = Duration.ofMillis(350);

	public static final int DEFAULT_MAX_RETRIES = 3;

	public static final double DEFAULT_RETRY_BACKOFF_SECONDS = 2.0;

	public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(30);

	private final String token;

	private final Duration throttleDelay;

	private final int maxRetries;

	private final double retryBackoffSeconds;

	private final Duration httpTimeout;

	private final URI baseUri;

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;

	private final RequestAuditSink auditSink;

	private final ReentrantLock throttleLock = new ReentrantLock();

	private long lastRequestAtNanos;

	public TushareClient(String token, RequestAuditSink auditSink) {
		this(token, DEFAULT_THROTTLE, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_BACKOFF_SECONDS, DEFAULT_HTTP_TIMEOUT,
				DEFAULT_BASE_URI, auditSink);
	}

	public TushareClient(String token, Duration throttleDelay, int maxRetries, double retryBackoffSeconds,
			Duration httpTimeout, URI baseUri, RequestAuditSink auditSink) {
		this.token = token;
		this.throttleDelay = throttleDelay;
		this.maxRetries = maxRetries;
		this.retryBackoffSeconds = retryBackoffSeconds;
		this.httpTimeout = httpTimeout;
		this.baseUri = baseUri;
		this.auditSink = auditSink;
		this.httpClient = HttpClient.newBuilder().connectTimeout(httpTimeout).build();
		this.objectMapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}

	/**
	 * 单次逻辑调用：翻页合并至数据取尽（has_more=false），整体成功/失败各回调一次审计。
	 * params 键值一律字符串/数字（Tushare 协议均为标量）。
	 */
	public List<TushareRow> fetch(String apiName, Map<String, Object> params) {
		String paramsJson = toJson(params);
		long startedAt = System.nanoTime();
		Map<String, Object> pageParams = new LinkedHashMap<>(params);
		List<TushareRow> rows = new ArrayList<>();
		int offset = 0;
		try {
			while (true) {
				if (offset > 0) {
					pageParams.put("offset", offset);
				}
				Envelope envelope = callWithRetry(apiName, pageParams);
				rows.addAll(envelope.rows());
				offset = rows.size();
				if (!envelope.hasMore()) {
					break;
				}
				log.info("tushare {} has_more=true, fetched {} rows, continuing with offset {}", apiName, offset,
						offset);
			}
			audit(apiName, paramsJson, "ok", rows.size(), elapsedMs(startedAt), null);
			return rows;
		}
		catch (RuntimeException e) {
			audit(apiName, paramsJson, "error", null, elapsedMs(startedAt), truncate(e.getMessage(), 500));
			throw e;
		}
	}

	/**
	 * 单次 HTTP 调用（含节流与重试），翻页循环在 {@link #fetch} 层。
	 */
	private Envelope callWithRetry(String apiName, Map<String, Object> params) {
		String body = toJson(Map.of("api_name", apiName, "token", token, "params", params, "fields", ""));
		RuntimeException lastError = null;
		for (int attempt = 0; attempt < maxRetries; attempt++) {
			throttle();
			try {
				return postOnce(apiName, body);
			}
			catch (RuntimeException e) {
				lastError = e;
				if (attempt < maxRetries - 1) {
					double base = retryBackoffSeconds * (attempt + 1)
							* (isRateLimitError(e.getMessage()) ? 1.0 : 0.5);
					double waitSeconds = base + ThreadLocalRandom.current().nextDouble(0.0, 1.0);
					log.warn("tushare retry {}/{} for {}: {} (sleep {}s)", attempt + 1, maxRetries, apiName,
							e.getMessage(), String.format("%.1f", waitSeconds));
					LockSupport.parkNanos((long) (waitSeconds * 1_000_000_000L));
				}
			}
		}
		throw lastError;
	}

	private Envelope postOnce(String apiName, String body) {
		HttpRequest request = HttpRequest.newBuilder(baseUri)
			.timeout(httpTimeout)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		String responseBody;
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new TushareClientException(
						"tushare http " + response.statusCode() + " for " + apiName + ": "
								+ truncate(response.body(), 200));
			}
			responseBody = response.body();
		}
		catch (IOException e) {
			throw new TushareClientException("tushare io error for " + apiName + ": " + e.getMessage(), e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TushareClientException("tushare call interrupted for " + apiName, e);
		}
		return parseEnvelope(apiName, responseBody);
	}

	private Envelope parseEnvelope(String apiName, String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		}
		catch (IOException e) {
			throw new TushareClientException("tushare bad json for " + apiName + ": " + truncate(responseBody, 200),
					e);
		}
		int code = root.path("code").asInt(-1);
		String msg = root.path("msg").asText(null);
		if (code != 0) {
			throw new TushareClientException("tushare api error for " + apiName + " code=" + code + " msg=" + msg);
		}
		JsonNode data = root.path("data");
		if (data.isMissingNode() || data.isNull()) {
			throw new TushareClientException("tushare empty data for " + apiName + ": " + truncate(responseBody, 200));
		}
		List<String> fields = new ArrayList<>();
		for (JsonNode field : data.path("fields")) {
			fields.add(field.asText());
		}
		List<TushareRow> rows = new ArrayList<>();
		for (JsonNode item : data.path("items")) {
			Map<String, JsonNode> cells = new LinkedHashMap<>();
			for (int i = 0; i < fields.size(); i++) {
				cells.put(fields.get(i), item.get(i));
			}
			rows.add(new TushareRow(cells));
		}
		boolean hasMore = data.path("has_more").asBoolean(false);
		return new Envelope(rows, hasMore);
	}

	private void throttle() {
		throttleLock.lock();
		try {
			long now = System.nanoTime();
			long waitNanos = throttleDelay.toNanos() - (now - lastRequestAtNanos);
			if (waitNanos > 0) {
				LockSupport.parkNanos(waitNanos);
			}
			lastRequestAtNanos = System.nanoTime();
		}
		finally {
			throttleLock.unlock();
		}
	}

	private boolean isRateLimitError(String message) {
		if (message == null) {
			return false;
		}
		for (String keyword : RATE_LIMIT_KEYWORDS) {
			if (message.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private void audit(String endpoint, String paramsJson, String status, Integer nRows, Long elapsedMs,
			String error) {
		if (auditSink == null) {
			return;
		}
		try {
			auditSink.accept(endpoint, "tushare", paramsJson, status, nRows, elapsedMs, error);
		}
		catch (RuntimeException e) {
			log.warn("tushare audit sink failed (ignored): {}", e.getMessage());
		}
	}

	private long elapsedMs(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / 1_000_000L;
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (IOException e) {
			throw new TushareClientException("tushare params serialize failed: " + e.getMessage(), e);
		}
	}

	private String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max);
	}

	private record Envelope(List<TushareRow> rows, boolean hasMore) {
	}

	// ------------------------------------------------------------------
	// 端点方法（params 构造照 Python tushare_client.py；窗口由调用方按 asOf 显式传入）
	// ------------------------------------------------------------------

	/** 全市场日行情（按交易日，daily，§4.3.4 批量日更） */
	public List<TushareRow> dailyByTradeDate(String yyyymmdd) {
		return fetch("daily", Map.of("trade_date", yyyymmdd));
	}

	/** 逐票日行情缺口补拉（daily） */
	public List<TushareRow> dailyByCode(String tsCode, String startDate, String endDate) {
		return fetch("daily", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 全市场复权因子（按交易日） */
	public List<TushareRow> adjFactorByTradeDate(String yyyymmdd) {
		return fetch("adj_factor", Map.of("trade_date", yyyymmdd));
	}

	/** 逐票复权因子缺口补拉 */
	public List<TushareRow> adjFactorByCode(String tsCode, String startDate, String endDate) {
		return fetch("adj_factor", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 指数日行情（index_daily；000300/000905/399006 三只） */
	public List<TushareRow> indexDaily(String tsCode, String startDate, String endDate) {
		return fetch("index_daily", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 交易日历全量/增量（trade_cal，SSE） */
	public List<TushareRow> tradeCal(String startDate, String endDate) {
		return fetch("trade_cal", Map.of("exchange", "SSE", "start_date", startDate, "end_date", endDate));
	}

	/** 股票基础信息全量（stock_basic，list_status=L） */
	public List<TushareRow> stockBasic() {
		return fetch("stock_basic", Map.of("exchange", "", "list_status", "L"));
	}

	/** 指数成分权重表（index_weight，跨多日快照，调用方取 max(trade_date)） */
	public List<TushareRow> indexWeight(String indexCode) {
		return fetch("index_weight", Map.of("index_code", indexCode));
	}

	/** 估值每日指标（daily_basic，腿2） */
	public List<TushareRow> dailyBasic(String tsCode, String startDate, String endDate) {
		return fetch("daily_basic", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 财务指标（fina_indicator，腿1——主文档 §4.3.7 允许的 THS 替代源；百分比数值入库层 ÷100 存小数） */
	public List<TushareRow> finaIndicator(String tsCode, String startDate, String endDate) {
		return fetch("fina_indicator", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 主力资金流（moneyflow，金额单位万元，入库层 ×1e4→元） */
	public List<TushareRow> moneyflow(String tsCode, String startDate, String endDate) {
		return fetch("moneyflow", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 龙虎榜（top_list，按日全市场一次，金额单位元） */
	public List<TushareRow> topList(String yyyymmdd) {
		return fetch("top_list", Map.of("trade_date", yyyymmdd));
	}

	/** 限售解禁（share_float，窗口 -3y~+2y 由调用方传） */
	public List<TushareRow> shareFloat(String tsCode, String startDate, String endDate) {
		return fetch("share_float", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

	/** 股东增减持（stk_holdertrade，窗口默认回看 365 日由调用方传） */
	public List<TushareRow> stkHoldertrade(String tsCode, String startDate, String endDate) {
		return fetch("stk_holdertrade", Map.of("ts_code", tsCode, "start_date", startDate, "end_date", endDate));
	}

}
