package com.mx.nqboard.sniper.data.provider.tencent;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TencentClient 解析语义单测——fqkline O-C-H-L、qfq key 回退 day、批量报价 GBK/万元→元。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class TencentClientTest {

	private HttpServer server;

	/** 响应队列（字节级：GBK 用例按 GBK 编码写入，与 client 解码口径一致） */
	private final List<byte[]> bodyQueue = new ArrayList<>();

	private final AtomicReference<String> lastQuery = new AtomicReference<>("");

	@BeforeEach
	void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			lastQuery.set(exchange.getRequestURI().toString());
			byte[] bytes = bodyQueue.isEmpty() ? "{}".getBytes(StandardCharsets.UTF_8) : bodyQueue.remove(0);
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	private void enqueueUtf8(String body) {
		bodyQueue.add(body.getBytes(StandardCharsets.UTF_8));
	}

	private void enqueueGbk(String body) {
		bodyQueue.add(body.getBytes(Charset.forName("GBK")));
	}

	private TencentClient client() {
		String base = "http://127.0.0.1:" + server.getAddress().getPort();
		// host 覆盖构造——严禁打真实外网
		return new TencentClient(null, base + "/appstock/app/fqkline/get", base + "/q=");
	}

	@Test
	@DisplayName("fqkline 行格式 O-C-H-L：[date,open,close,high,low,vol(手)]；param 含 640 与连字符日期")
	void fqklineOchlAndParamFormat() {
		enqueueUtf8("""
				{"code":0,"data":{"sh600519":{"day":[
				  ["2026-09-30","10.0","11.0","11.5","9.8","12345"],
				  ["2026-10-08","11.2","12.0","12.4","11.0","15000"]
				]}}}""");

		List<KlineBar> bars = client().fqkline("600519", "SH", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 9),
				"none");

		assertThat(bars).hasSize(2);
		// O-C-H-L：open=row[1]、close=row[2]、high=row[3]、low=row[4]
		assertThat(bars.get(0).open()).isEqualByComparingTo("10.0");
		assertThat(bars.get(0).close()).isEqualByComparingTo("11.0");
		assertThat(bars.get(0).high()).isEqualByComparingTo("11.5");
		assertThat(bars.get(0).low()).isEqualByComparingTo("9.8");
		assertThat(bars.get(0).volume()).isEqualByComparingTo("12345");
		assertThat(bars.get(0).amount()).isNull();
		// param 断言：symbol,day,起,止,640（fq 为空不带尾参）
		assertThat(lastQuery.get()).contains("param=sh600519%2Cday%2C2026-09-01%2C2026-10-09%2C640");
	}

	@Test
	@DisplayName("qfq 请求 key=qfqday，无数据回退 day")
	void qfqKeyFallsBackToDay() {
		enqueueUtf8("""
				{"code":0,"data":{"sh600519":{"day":[
				  ["2026-09-30","900.0","910.0","915.0","895.0","8000"]
				]}}}""");

		List<KlineBar> bars = client().fqkline("600519", "SH", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				"qfq");

		assertThat(bars).hasSize(1);
		assertThat(bars.get(0).volume()).isEqualByComparingTo("8000");
		assertThat(lastQuery.get()).contains("%2Cqfq");
	}

	@Test
	@DisplayName("批量报价 GBK 位置映射：37=成交额(万元×1e4→元)、代码补零")
	void batchQuotesGbkPositions() {
		// payload 按 ~ 分割：0=市场前缀 1=名称 2=代码 3=最新 4=昨收 5=今开 32=涨跌幅 33=最高 34=最低
		// 36=总手(手) 37=成交额(万元) 38=换手率——按索引精确对位构造 fixture
		String[] parts = new String[40];
		parts[0] = "51";
		parts[1] = "贵州茅台";
		parts[2] = "600519";
		parts[3] = "1800.00";
		parts[4] = "1770.00";
		parts[5] = "1790.00";
		parts[32] = "5.10";
		parts[33] = "1810.00";
		parts[34] = "1780.00";
		parts[36] = "51000";
		parts[37] = "36000000.00";
		parts[38] = "0.80";
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] == null) {
				parts[i] = "";
			}
		}
		enqueueGbk("v_sh600519=\"" + String.join("~", parts) + "\";");

		List<QuoteSnapshot> quotes = client().batchQuotes(List.of("sh600519"));

		assertThat(quotes).hasSize(1);
		QuoteSnapshot q = quotes.get(0);
		assertThat(q.code()).isEqualTo("600519");
		assertThat(q.name()).isEqualTo("贵州茅台");
		assertThat(q.price()).isEqualByComparingTo("1800.00");
		assertThat(q.prevClose()).isEqualByComparingTo("1770.00");
		assertThat(q.open()).isEqualByComparingTo("1790.00");
		assertThat(q.changePct()).isEqualByComparingTo("5.10");
		assertThat(q.high()).isEqualByComparingTo("1810.00");
		assertThat(q.low()).isEqualByComparingTo("1780.00");
		assertThat(q.volume()).isEqualByComparingTo("51000");
		// 万元 ×1e4 → 元
		assertThat(q.amount()).isEqualByComparingTo("360000000000");
		assertThat(q.turnoverRate()).isEqualByComparingTo("0.80");
	}

}
