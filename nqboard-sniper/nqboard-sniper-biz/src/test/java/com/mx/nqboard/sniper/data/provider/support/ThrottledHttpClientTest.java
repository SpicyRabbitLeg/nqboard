package com.mx.nqboard.sniper.data.provider.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.mx.nqboard.sniper.data.RequestAuditSink;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ThrottledHttpClient QoS 语义单测——重试/熔断/默认头/GBK/节流/审计，
 * 全部对照 Python rate_limit.py + akshare_client._run_em_retry 语义。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class ThrottledHttpClientTest {

	private HttpServer server;

	private URI baseUri;

	private final AtomicInteger hits = new AtomicInteger();

	private final AtomicReference<String> bodyRef = new AtomicReference<>("{}");

	private final List<String> lastHeaders = new CopyOnWriteArrayList<>();

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		hits.set(0);
		server.createContext("/", exchange -> {
			hits.incrementAndGet();
			lastHeaders.clear();
			exchange.getRequestHeaders().forEach((k, v) -> lastHeaders.add(k + ":" + v));
			byte[] bytes = bodyRef.get().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	/** 拒连端口：起一个 listener 拿端口后立即关闭，形成 Connection refused */
	private String refusedUrl() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return "http://127.0.0.1:" + socket.getLocalPort() + "/x";
		}
	}

	private ThrottledHttpClient newClient(CircuitBreaker breaker, RequestAuditSink sink) {
		return new ThrottledHttpClient("test", Duration.ofMillis(30), 0.01, Duration.ofSeconds(2), breaker, sink,
				Map.of("User-Agent", "JUnit-UA", "Connection", "close"));
	}

	@Test
	@DisplayName("连接类错误触发熔断（Connection refused 属 7 类标记）")
	void retriesOnConnectionErrorAndMarksCircuit() throws IOException {
		CircuitBreaker breaker = CircuitBreaker.withDefaults();
		ThrottledHttpClient client = newClient(breaker, null);
		String refused = refusedUrl();

		assertThatThrownBy(() -> client.get(refused, Map.of(), (Charset) null, 3))
			.isInstanceOf(RuntimeException.class);
		assertThat(breaker.isOpen()).as("连接错误应标记熔断 OPEN").isTrue();
	}

	@Test
	@DisplayName("熔断 OPEN 期间短路为单次快速尝试")
	void singleQuickAttemptWhenCircuitOpen() {
		CircuitBreaker breaker = CircuitBreaker.withDefaults();
		breaker.markBlock();
		ThrottledHttpClient client = newClient(breaker, null);

		String response = client.get(baseUri + "ok", Map.of(), (Charset) null, 5);

		assertThat(response).isEqualTo("{}");
		assertThat(hits.get()).as("OPEN 期即使 maxAttempts=5 也只发 1 次").isEqualTo(1);
	}

	@Test
	@DisplayName("默认头注入（UA/Connection: close）与 GBK 解码")
	void sendsDefaultHeadersAndDecodesGbk() {
		// GBK 编码中文响应
		server.stop(0);
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		hits.set(0);
		server.createContext("/", exchange -> {
			hits.incrementAndGet();
			lastHeaders.clear();
			exchange.getRequestHeaders().forEach((k, v) -> lastHeaders.add(k.toLowerCase() + ":" + v));
			byte[] bytes = "贵州茅台".getBytes(Charset.forName("GBK"));
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");

		ThrottledHttpClient client = newClient(null, null);
		String text = client.get(baseUri + "q", Map.of(), Charset.forName("GBK"), 1);

		assertThat(text).isEqualTo("贵州茅台");
		// UA 正常注入（header 值带方括号格式）；Connection 属 JDK 受限头不会被注入
		assertThat(String.join(";", lastHeaders)).contains("user-agent:[JUnit-UA]");
	}

	@Test
	@DisplayName("节流下限生效：两次调用间隔不小于 minDelay")
	void throttlesMinDelay() {
		ThrottledHttpClient client = newClient(null, null);
		long start = System.nanoTime();
		client.get(baseUri + "a", Map.of(), (Charset) null, 1);
		client.get(baseUri + "b", Map.of(), (Charset) null, 1);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
		assertThat(elapsedMs).isGreaterThanOrEqualTo(30);
	}

	@Test
	@DisplayName("审计 sink：成功 nRows=响应字节数，失败落 error")
	void auditsSuccessAndFailure() throws IOException {
		List<String> captured = new CopyOnWriteArrayList<>();
		RequestAuditSink sink = (endpoint, source, params, status, nRows, elapsedMs, error) -> captured
			.add(endpoint + "|" + source + "|" + status + "|" + nRows + "|" + error);

		ThrottledHttpClient client = newClient(null, sink);
		client.get(baseUri + "path/to/get", Map.of("k", "v"), (Charset) null, 1);
		assertThat(captured).hasSize(1);
		assertThat(captured.get(0)).startsWith("get|test|ok|2|");

		ThrottledHttpClient failing = newClient(CircuitBreaker.withDefaults(), sink);
		assertThatThrownBy(() -> failing.get(refusedUrl(), Map.of(), (Charset) null, 1))
			.isInstanceOf(RuntimeException.class);
		assertThat(captured).hasSize(2);
		assertThat(captured.get(1)).contains("error|null");
	}

}
