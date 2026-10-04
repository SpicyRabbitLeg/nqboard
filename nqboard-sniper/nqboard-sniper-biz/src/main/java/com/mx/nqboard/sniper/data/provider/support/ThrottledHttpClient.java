package com.mx.nqboard.sniper.data.provider.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import com.mx.nqboard.sniper.data.RequestAuditSink;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 兜底源（东财/腾讯/新浪）共享 GET 层——语义照搬三处 Python 实现：
 * </p>
 * <ul>
 * <li>节流：全局锁内按"请求开始时刻"锚定最小间隔，下限 500ms（{@code rate_limit.throttle}：max(delay, 0.5)）</li>
 * <li>重试：{@code run_throttled_retry}——退避 {@code BACKOFF*(n) + rand(0,1)} 秒（无 Tushare 的 0.5 系数）；
 * 尝试次数逐端点照源码（EM K线 3 次、EM 其余/腾讯/新浪多数 2 次、腾讯K线 3 次）</li>
 * <li>熔断（仅东财）：连接类错误 → OPEN 300s；OPEN 期间短路为单次快速尝试，仍连接错误则续期
 * （{@code _run_em_retry}/{@code _em_circuit_open} 语义）</li>
 * <li>连接错误判定：异常 message 含 7 类标记（Connection aborted/RemoteDisconnected/timed out/
 * Max retries exceeded/Failed to resolve/Connection reset/Connection refused，{@code _EM_CONN_ERROR_MARKERS}）；
 * HTTP 状态错误（raise_for_status 语义）不算连接错误、不触发熔断</li>
 * <li>审计：每次逻辑调用成功/失败各回调一次；nRows 记响应字节数（Python _get 记 {@code len(content)B}，对拍可比）</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public final class ThrottledHttpClient {

	/** 连接类错误标记（照 akshare_client._EM_CONN_ERROR_MARKERS 7 类；"Failed to connect" 为 JDK ConnectException 消息的对应补充） */
	private static final String[] CONNECTION_ERROR_MARKERS = { "Connection aborted", "RemoteDisconnected",
			"timed out", "Max retries exceeded", "Failed to resolve", "Connection reset", "Connection refused",
			"Failed to connect" };

	/** JDK HttpClient 受限头（构造请求时非法），设置默认头前过滤——Python 的 Connection: close 语义由 JDK stale 连接自动重试兜底 */
	private static final String[] RESTRICTED_HEADERS = { "connection", "content-length", "expect", "host", "upgrade" };

	public static final Duration DEFAULT_MIN_DELAY = Duration.ofMillis(500);

	public static final double DEFAULT_BACKOFF_SECONDS = 2.0;

	public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(20);

	private final Duration minDelay;

	private final double backoffSeconds;

	private final Duration httpTimeout;

	private final CircuitBreaker circuitBreaker;

	private final RequestAuditSink auditSink;

	private final String source;

	private final HttpClient httpClient;

	private final Map<String, String> defaultHeaders;

	private final ReentrantLock throttleLock = new ReentrantLock();

	private long lastRequestAtNanos;

	public ThrottledHttpClient(String source, CircuitBreaker circuitBreaker, RequestAuditSink auditSink) {
		this(source, DEFAULT_MIN_DELAY, DEFAULT_BACKOFF_SECONDS, DEFAULT_HTTP_TIMEOUT, circuitBreaker, auditSink,
				Map.of());
	}

	public ThrottledHttpClient(String source, Duration minDelay, double backoffSeconds, Duration httpTimeout,
			CircuitBreaker circuitBreaker, RequestAuditSink auditSink, Map<String, String> defaultHeaders) {
		this.source = source;
		this.minDelay = minDelay;
		this.backoffSeconds = backoffSeconds;
		this.httpTimeout = httpTimeout;
		this.circuitBreaker = circuitBreaker;
		this.auditSink = auditSink;
		this.defaultHeaders = Map.copyOf(defaultHeaders);
		// 强制 HTTP/1.1（对齐 Python requests，避免 h2c Upgrade 头）
		this.httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	}

	/**
	 * 带参 GET（query 编码）+ 重试/熔断/审计。
	 * @param url 无 query 的完整 URL
	 * @param queryParams query 参数（null 值跳过；值 URLEncoder 编码）
	 * @param forceCharset 非空时按该字符集解码响应体（腾讯/新浪 GBK）
	 * @param maxAttempts 总尝试次数（逐端点照 Python 源码：EM 多数 2、EM K线 3、腾讯/新浪 K线 3、报价 2）
	 * @return 响应体文本
	 */
	public String get(String url, Map<String, String> queryParams, Charset forceCharset, int maxAttempts) {
		String fullUrl = appendQuery(url, queryParams);
		String endpoint = endpointOf(url);
		long startedAt = System.nanoTime();
		RuntimeException lastError = null;
		try {
			int attempts = effectiveAttempts(maxAttempts);
			for (int attempt = 0; attempt < attempts; attempt++) {
				throttle();
				try {
					String body = doGet(fullUrl, forceCharset);
					audit(endpoint, fullUrl, "ok", body.length(), elapsedMs(startedAt), null);
					return body;
				}
				catch (HttpStatusException e) {
					// raise_for_status 语义：HTTP 状态错误非连接错误，不触发熔断、照常重试
					lastError = e;
					retryPause(endpoint, attempt, attempts, e);
				}
				catch (IOException e) {
					lastError = new TransientHttpException(fullUrl, e.getMessage(), e);
					// JDK ConnectException（消息 "Failed to connect to ..."）按类型判定 + 8 类 message 标记双保险
					if (circuitBreaker != null && (e instanceof java.net.ConnectException || isConnectionError(e.getMessage()))) {
						circuitBreaker.markBlock();
						log.warn("{} endpoints failing; circuit open {}s (fallbacks engage immediately)", source,
								circuitBreaker.blockSeconds(), e);
					}
					retryPause(endpoint, attempt, attempts, lastError);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			lastError = new TransientHttpException(fullUrl, "interrupted", e);
		}
		if (circuitBreaker != null && circuitBreaker.isOpen() && lastError instanceof TransientHttpException
				&& isConnectionError(lastError.getMessage())) {
			// OPEN 期单次快速尝试仍连接错误：续期熔断（_run_em_retry OPEN 分支语义）
			circuitBreaker.markBlock();
		}
		audit(endpoint, fullUrl, "error", null, elapsedMs(startedAt), truncate(lastError.getMessage(), 500));
		throw lastError;
	}

	private int effectiveAttempts(int configured) {
		if (circuitBreaker != null && circuitBreaker.isOpen()) {
			// OPEN 短路：单次快速尝试，防每个请求烧满重试时长（_run_em_retry 语义）
			return 1;
		}
		return configured;
	}

	private void retryPause(String endpoint, int attempt, int attempts, RuntimeException error)
			throws InterruptedException {
		if (attempt < attempts - 1) {
			double wait = backoffSeconds * (attempt + 1) + ThreadLocalRandom.current().nextDouble(0.0, 1.0);
			log.warn("{} retry {}/{} for {} after error: {} (sleep {}s)", source, attempt + 1, attempts, endpoint,
					error.getMessage(), String.format("%.1f", wait));
			LockSupport.parkNanos((long) (wait * 1_000_000_000L));
		}
	}

	private String doGet(String fullUrl, Charset forceCharset) throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(fullUrl))
			.timeout(httpTimeout)
			.GET();
		defaultHeaders.forEach((name, value) -> {
			if (!isRestrictedHeader(name)) {
				builder.setHeader(name, value);
			}
		});
		HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() != 200) {
			throw new HttpStatusException(fullUrl, response.statusCode());
		}
		Charset charset = forceCharset != null ? forceCharset : StandardCharsets.UTF_8;
		return new String(response.body(), charset);
	}

	private boolean isRestrictedHeader(String name) {
		for (String restricted : RESTRICTED_HEADERS) {
			if (restricted.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private void throttle() {
		throttleLock.lock();
		try {
			long now = System.nanoTime();
			long waitNanos = minDelay.toNanos() - (now - lastRequestAtNanos);
			if (waitNanos > 0) {
				LockSupport.parkNanos(waitNanos);
			}
			lastRequestAtNanos = System.nanoTime();
		}
		finally {
			throttleLock.unlock();
		}
	}

	private boolean isConnectionError(String message) {
		if (message == null) {
			return false;
		}
		for (String marker : CONNECTION_ERROR_MARKERS) {
			if (message.contains(marker)) {
				return true;
			}
		}
		return false;
	}

	private void audit(String endpoint, String url, String status, Integer nRows, Long elapsedMs, String error) {
		if (auditSink == null) {
			return;
		}
		try {
			auditSink.accept(endpoint, source, url, status, nRows, elapsedMs, error);
		}
		catch (RuntimeException e) {
			log.warn("{} audit sink failed (ignored): {}", source, e.getMessage());
		}
	}

	private long elapsedMs(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / 1_000_000L;
	}

	private String appendQuery(String url, Map<String, String> queryParams) {
		if (queryParams == null || queryParams.isEmpty()) {
			return url;
		}
		StringBuilder sb = new StringBuilder(url).append('?');
		boolean first = true;
		for (Map.Entry<String, String> entry : queryParams.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			if (!first) {
				sb.append('&');
			}
			first = false;
			sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
				.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	/** 端点标识（照 Python _get：path 尾段，如 kline/get、clist/get、getKLineData） */
	private String endpointOf(String url) {
		String path = URI.create(url).getPath();
		int slash = path.lastIndexOf('/');
		String last = slash >= 0 ? path.substring(slash + 1) : path;
		return last.isEmpty() ? URI.create(url).getHost() : last;
	}

	private String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max);
	}

	/** HTTP 状态错误（raise_for_status 语义，不触发熔断） */
	public static class HttpStatusException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private final int statusCode;

		public HttpStatusException(String url, int statusCode) {
			super("http " + statusCode + " for " + url);
			this.statusCode = statusCode;
		}

		public int statusCode() {
			return statusCode;
		}

	}

	/** 连接/IO 类瞬态错误（7 类标记判定输入） */
	public static class TransientHttpException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public TransientHttpException(String url, String message, Throwable cause) {
			super("io error for " + url + ": " + message, cause);
		}

	}

}
