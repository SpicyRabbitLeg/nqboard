package com.mx.nqboard.sniper.data.provider.tushare;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.data.RequestAuditSink;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TushareClient 表驱动单测——内嵌 JDK HttpServer 回放信封（正常/翻页/限频/审计），
 * 覆盖 Python _call/_throttle 的照搬语义。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class TushareClientTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private HttpServer server;

	private URI baseUri;

	private final List<String> requestBodies = new ArrayList<>();

	private final AtomicReference<java.util.function.Function<String, String>> responder = new AtomicReference<>();

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		// 固定 context + 可替换 handler：JDK HttpServer 对同路径 createContext 不做替换，改为引用切换
		server.createContext("/", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			requestBodies.add(body);
			String response = responder.get().apply(body);
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
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

	private void respond(java.util.function.Function<String, String> handler) {
		responder.set(handler);
	}

	private TushareClient newClient(RequestAuditSink sink) {
		// 加速参数：节流 50ms、退避 0.05s——重试语义照 Python，退避时长缩到毫秒级仅为测试提速
		return new TushareClient("test-token", Duration.ofMillis(50), 3, 0.05, Duration.ofSeconds(2), baseUri, sink);
	}

	@Test
	@DisplayName("信封按 fields 名解析，数值经 BigDecimal 保精度")
	void fetchParsesByFieldNamesAndKeepsDecimalPrecision() {
		respond(body -> """
				{"code":0,"msg":null,"data":{
				  "fields":["ts_code","trade_date","vol","factor"],
				  "items":[["600519.SH","20260930",123456,"32.4982100000"]],
				  "has_more":false}}""");
		TushareClient client = newClient(null);

		List<TushareRow> rows = client.dailyByCode("600519.SH", "20260901", "20260930");

		assertThat(rows).hasSize(1);
		TushareRow row = rows.get(0);
		assertThat(row.str("ts_code")).isEqualTo("600519.SH");
		assertThat(row.date("trade_date")).isEqualTo(LocalDate.of(2026, 9, 30));
		// 精度红线：factor 原文 32.4982100000 不得经 double 舍入
		assertThat(row.dec("factor")).isEqualByComparingTo(new BigDecimal("32.4982100000"));
		assertThat(row.dec("factor")).hasScaleOf(10);
		assertThat(row.dec("vol")).isEqualByComparingTo(new BigDecimal("123456"));
		assertThat(row.str("missing")).isNull();
		assertThat(row.dec("missing")).isNull();
	}

	@Test
	@DisplayName("has_more=true 带 offset 翻页重调，行数累计合并")
	void fetchPaginatesWhenHasMore() {
		respond(body -> {
			try {
				JsonNode params = MAPPER.readTree(body).path("params");
				if (!params.has("offset")) {
					return """
							{"code":0,"msg":null,"data":{
							  "fields":["ts_code"],
							  "items":[["000001.SZ"],["000002.SZ"]],
							  "has_more":true}}""";
				}
				assertThat(params.path("offset").asInt()).isEqualTo(2);
				return """
						{"code":0,"msg":null,"data":{
						  "fields":["ts_code"],
						  "items":[["000003.SZ"]],
						  "has_more":false}}""";
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		TushareClient client = newClient(null);

		List<TushareRow> rows = client.fetch("daily", Map.of("trade_date", "20260930"));

		assertThat(rows).hasSize(3);
		assertThat(requestBodies).hasSize(2);
		assertThat(requestBodies.get(0)).contains("\"api_name\":\"daily\"").contains("\"token\":\"test-token\"");
		assertThat(requestBodies.get(1)).contains("\"offset\":2");
	}

	@Test
	@DisplayName("限频错误共 3 次总尝试后抛出（照 _call 语义）")
	void retriesThreeAttemptsOnRateLimitError() {
		respond(body -> "{\"code\":40110,\"msg\":\"抱歉，您每分钟最多访问该接口1次\",\"data\":null}");
		TushareClient client = newClient(null);

		assertThatThrownBy(() -> client.dailyByTradeDate("20260930")).isInstanceOf(TushareClientException.class)
			.hasMessageContaining("每分钟");
		// _call: for attempt in range(3) —— 共 3 次尝试，不是 1 初试 + 3 重试
		assertThat(requestBodies).hasSize(3);
	}

	@Test
	@DisplayName("全局节流：两次逻辑调用间隔不小于配置延迟")
	void throttlesGlobalInterval() {
		respond(body -> """
				{"code":0,"msg":null,"data":{"fields":["ts_code"],"items":[["000001.SZ"]],"has_more":false}}""");
		TushareClient client = newClient(null);

		long start = System.nanoTime();
		client.fetch("daily", Map.of("trade_date", "20260929"));
		client.fetch("daily", Map.of("trade_date", "20260930"));
		long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

		// 第二次调用至少等待 50ms 节流窗口（HTTP 本机耗时远小于 50ms，断言有余量）
		assertThat(elapsedMs).isGreaterThanOrEqualTo(50);
	}

	@Test
	@DisplayName("审计 sink：成功/失败各落一条（log_request 粒度，翻页合并为一条）")
	void auditsSuccessAndFailure() {
		List<String> captured = new CopyOnWriteArrayList<>();
		respond(body -> {
			try {
				JsonNode params = MAPPER.readTree(body).path("params");
				if (!params.has("offset")) {
					return """
							{"code":0,"msg":null,"data":{"fields":["ts_code"],"items":[["000001.SZ"],["000002.SZ"]],"has_more":true}}""";
				}
				return """
						{"code":0,"msg":null,"data":{"fields":["ts_code"],"items":[["000003.SZ"]],"has_more":false}}""";
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		TushareClient successClient = newClient(
				(endpoint, source, params, status, nRows, elapsedMs, error) -> captured
					.add(endpoint + "|" + source + "|" + status + "|" + nRows));
		successClient.fetch("daily", Map.of("trade_date", "20260930"));

		assertThat(captured).hasSize(1);
		assertThat(captured.get(0)).isEqualTo("daily|tushare|ok|3");

		respond(body -> "{\"code\":40110,\"msg\":\"抱歉，您每分钟最多访问该接口1次\",\"data\":null}");
		assertThatThrownBy(() -> newClient(
				(endpoint, source, params, status, nRows, elapsedMs, error) -> captured
					.add(endpoint + "|" + source + "|" + status + "|" + error))
			.fetch("top_list", Map.of("trade_date", "20260930"))).isInstanceOf(TushareClientException.class);

		assertThat(captured).hasSize(2);
		assertThat(captured.get(1)).contains("top_list|tushare|error|").contains("每分钟");
	}

	@Test
	@DisplayName("行访问器：日期双格式、字符串数字、nan/null 归一")
	void rowAccessorsNormalize() {
		respond(body -> """
				{"code":0,"msg":null,"data":{
				  "fields":["ann_date","float_ratio","net_mf_amount","holder_name"],
				  "items":[["2026-09-30","8.0","nan",null],["20260929",null,"123.45","张三"]],
				  "has_more":false}}""");
		TushareClient client = newClient(null);

		List<TushareRow> rows = client.fetch("share_float", Map.of("ts_code", "600519.SH"));

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).date("ann_date")).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(rows.get(1).date("ann_date")).isEqualTo(LocalDate.of(2026, 9, 29));
		assertThat(rows.get(0).dec("float_ratio")).isEqualByComparingTo(new BigDecimal("8.0"));
		assertThat(rows.get(1).dec("float_ratio")).isNull();
		assertThat(rows.get(0).dec("net_mf_amount")).isNull();
		assertThat(rows.get(1).dec("net_mf_amount")).isEqualByComparingTo(new BigDecimal("123.45"));
		assertThat(rows.get(0).str("holder_name")).isNull();
		assertThat(rows.get(1).str("holder_name")).isEqualTo("张三");
	}

}
