package com.mx.nqboard.sniper.data.provider.sina;

import java.math.BigDecimal;
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
 * SinaClient 解析语义单测——K线 股→手、hq.sinajs 字段位置与涨跌幅现算、行情中心分页终止。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class SinaClientTest {

	private HttpServer server;

	/** 响应队列（字节级：GBK 用例按 GBK 编码写入，与 client 解码口径一致） */
	private final List<byte[]> bodyQueue = new ArrayList<>();

	private final AtomicReference<String> referer = new AtomicReference<>("");

	@BeforeEach
	void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			referer.set(exchange.getRequestHeaders().getFirst("Referer"));
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

	private SinaClient client() {
		String base = "http://127.0.0.1:" + server.getAddress().getPort();
		// host 覆盖构造——严禁打真实外网
		return new SinaClient(null, base + "/CN_MarketDataService.getKLineData", base + "/list=",
				base + "/Market_Center.getHQNodeData");
	}

	@Test
	@DisplayName("K线仅不复权：volume 股→÷100 手、窗口过滤、带 Sina Referer")
	void klineConvertsSharesToHands() {
		enqueueUtf8("""
				[{"day":"2026-09-29","open":"10.0","high":"10.5","low":"9.9","close":"10.2","volume":"1234000"},
				 {"day":"2026-09-30","open":"10.2","high":"10.8","low":"10.1","close":"10.6","volume":"2000000"},
				 {"day":"2026-10-08","open":"10.8","high":"11.0","low":"10.7","close":"10.9","volume":"1500000"}]""");

		List<KlineBar> bars = client().kline("600519", "SH", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

		// 10-08 在窗口外被过滤；volume 1234000 股 ÷100 = 12340 手
		assertThat(bars).hasSize(2);
		assertThat(bars.get(0).volume()).isEqualByComparingTo("12340");
		assertThat(bars.get(1).volume()).isEqualByComparingTo("20000");
		assertThat(bars.get(0).close()).isEqualByComparingTo("10.2");
		assertThat(bars.get(0).amount()).isNull();
		assertThat(referer.get()).isEqualTo("https://finance.sina.com.cn");
	}

	@Test
	@DisplayName("批量报价：GBK 字节解码，字段位置 0=名称 1=今开 2=昨收 3=最新 8=成交量(股→手) 9=成交额(元)，涨跌幅现算")
	void batchQuotesPositionsAndDerivedPct() {
		// hq.sinajs 位置：0 名称 1 今开 2 昨收 3 最新 4 最高 5 最低 … 8 成交量(股) 9 成交额(元) 30 日期 …
		StringBuilder line = new StringBuilder("var hq_str_sh600519=\"贵州茅台,1790.00,1770.00,1800.00,1810.00,1780.00,"
				+ "1799.99,1800.01,2000000,3600000000");
		// 补齐至 ≥31 字段
		for (int i = 0; i < 22; i++) {
			line.append(",0");
		}
		line.append(",2026-09-30,15:00:00\";\n");
		enqueueGbk(line.toString());

		List<QuoteSnapshot> quotes = client().batchQuotes(List.of("sh600519"));

		assertThat(quotes).hasSize(1);
		QuoteSnapshot q = quotes.get(0);
		assertThat(q.name()).isEqualTo("贵州茅台");
		assertThat(q.price()).isEqualByComparingTo("1800.00");
		assertThat(q.prevClose()).isEqualByComparingTo("1770.00");
		assertThat(q.open()).isEqualByComparingTo("1790.00");
		// 涨跌幅现算：(1800-1770)/1770*100 ≈ 1.6949%
		assertThat(q.changePct()).isEqualByComparingTo(new BigDecimal("1.6949152542"));
		// 成交量 2000000 股 → 20000 手
		assertThat(q.volume()).isEqualByComparingTo("20000");
		assertThat(q.amount()).isEqualByComparingTo("3600000000");
		assertThat(q.turnoverRate()).isNull();
	}

	@Test
	@DisplayName("行情中心分页：不足一页即终止、changepercent 兜底、volume 股→手")
	void marketSpotPaginatesUntilShortPage() {
		enqueueUtf8("""
				[{"code":"600519","name":"贵州茅台","trade":"1800.0","settlement":"1770.0",
				  "changepercent":"1.69","amount":"3600000000","volume":"2000000",
				  "open":"1790.0","high":"1810.0","low":"1780.0"}]""");

		List<QuoteSnapshot> snapshots = client().marketSpot(3);

		assertThat(snapshots).hasSize(1);
		QuoteSnapshot q = snapshots.get(0);
		assertThat(q.code()).isEqualTo("600519");
		// trade/settlement 非空 → 现算优先：(1800-1770)/1770*100 ≈ 1.695 ≠ 1.69
		assertThat(q.changePct()).isEqualByComparingTo(new BigDecimal("1.6949152542"));
		assertThat(q.volume()).isEqualByComparingTo("20000");
	}

}
