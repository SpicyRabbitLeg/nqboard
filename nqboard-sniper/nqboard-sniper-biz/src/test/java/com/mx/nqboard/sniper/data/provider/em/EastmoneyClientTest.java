package com.mx.nqboard.sniper.data.provider.em;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.NewsItem;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EastmoneyClient 解析语义单测（内嵌 server + host 覆盖构造）。
 * 重点：K线 CSV 行序 O-C-H-L、clist 翻页与 f3 降序、jsonp 剥壳去标签。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class EastmoneyClientTest {

	private HttpServer server;

	/** 响应队列：boardCodes/boardKline 等多请求场景按序取 body（boardCodes 无缓存每次现拉） */
	private final List<String> bodyQueue = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			String body = bodyQueue.isEmpty() ? "{}" : bodyQueue.remove(0);
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
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

	private void enqueue(String body) {
		bodyQueue.add(body);
	}

	/** 五类 host 全部指向内嵌 server（单接口场景逐用例注入 body） */
	private EastmoneyClient client() {
		String host = "http://127.0.0.1:" + server.getAddress().getPort();
		return new EastmoneyClient(null, null, host, host, host, host, host);
	}

	@Test
	@DisplayName("K线 CSV 行序 O-C-H-L：开盘=p1 收盘=p2 最高=p3 最低=p4（误按 O-H-L-C 解析会错位）")
	void klineOchlOrdering() {
		enqueue("""
				{"data":{"code":"600519","klines":["2026-09-30,10.0,11.0,11.5,9.8,12345,1234560.0,1.2,3.1,0.9,5.5"]}}""");

		List<KlineBar> bars = client().stockKline("600519", "0", "20260901", "20260930");

		assertThat(bars).hasSize(1);
		KlineBar bar = bars.get(0);
		assertThat(bar.open()).isEqualByComparingTo("10.0");
		assertThat(bar.close()).isEqualByComparingTo("11.0");
		assertThat(bar.high()).isEqualByComparingTo("11.5");
		assertThat(bar.low()).isEqualByComparingTo("9.8");
		assertThat(bar.volume()).isEqualByComparingTo("12345");
		assertThat(bar.amount()).isEqualByComparingTo("1234560.0");
		assertThat(bar.preClose()).isNull();
		assertThat(bar.source()).isEqualTo("em");
	}

	@Test
	@DisplayName("clist 快照：total 求总页数合并翻页，结果按 f3 降序")
	void spotAllPaginatesAndSorts() {
		// page1: 2 行 total=2（1 页收敛）；f3=5.1 与 f3=-2.0 → 降序后 5.1 在前
		enqueue("""
				{"data":{"total":2,"diff":[
				  {"f12":"000001","f14":"平安银行","f2":12.3,"f3":-2.0,"f5":100,"f6":123000,"f8":1.2,
				   "f15":12.9,"f16":11.8,"f17":12.0,"f18":11.7},
				  {"f12":"600519","f14":"贵州茅台","f2":1800.0,"f3":5.1,"f5":20000,"f6":36000000,
				   "f8":0.8,"f15":1810.0,"f16":1780.0,"f17":1790.0,"f18":1770.0}
				]}}""");

		List<QuoteSnapshot> snapshots = client().spotAll();

		assertThat(snapshots).hasSize(2);
		assertThat(snapshots.get(0).code()).isEqualTo("600519");
		assertThat(snapshots.get(0).changePct()).isEqualByComparingTo("5.1");
		assertThat(snapshots.get(1).code()).isEqualTo("000001");
		assertThat(snapshots.get(1).prevClose()).isEqualByComparingTo("11.7");
	}

	@Test
	@DisplayName("jsonp 新闻：剥壳、去 <em> 标签、mediaName 空默认东方财富、date 截前 10 位")
	void searchNewsStripsJsonpAndEmTags() {
		enqueue("""
				jQuerycb({"result":{"cmsArticleWebOld":[
				  {"title":"<em>利好</em>公告","content":"正文<em>重点</em>","date":"2026-09-30 10:00:00",
				   "url":"http://x/1","mediaName":"证券时报"},
				  {"title":"t2","content":"c2","date":"2026-09-29","url":"http://x/2","mediaName":""}
				]}})""");

		List<NewsItem> items = client().searchNews("600519", 10);

		assertThat(items).hasSize(2);
		assertThat(items.get(0).title()).isEqualTo("利好公告");
		assertThat(items.get(0).content()).isEqualTo("正文重点");
		assertThat(items.get(0).date()).isEqualTo("2026-09-30");
		assertThat(items.get(0).sourceName()).isEqualTo("证券时报");
		assertThat(items.get(1).sourceName()).isEqualTo("东方财富");
	}

	@Test
	@DisplayName("f127 行业：push2delay 与 push2 双 host，'-','--','nan' 不采用")
	void industryFiltersPlaceholders() {
		enqueue("{\"data\":{\"f57\":\"600519\",\"f58\":\"贵州茅台\",\"f127\":\"电池\"}}");
		assertThat(client().industryByCode("600519")).isEqualTo("电池");

		enqueue("{\"data\":{\"f127\":\"--\"}}");
		assertThat(client().industryByCode("600519")).isNull();

		enqueue("{\"data\":{\"f127\":\"nan\"}}");
		assertThat(client().industryByCode("600519")).isNull();
	}

	@Test
	@DisplayName("板块代码表：翻页合并与互为子串模糊匹配；boardKline 内部二次拉板块表（无缓存语义）")
	void boardCodesFuzzyMatch() {
		String boardList = """
				{"data":{"total":2,"diff":[
				  {"f12":"BK0479","f14":"电池"},
				  {"f12":"BK0475","f14":"银行"}
				]}}""";
		// client.boardCodes() 无缓存（缓存归上层网关），boardCodes 与 boardKline 各拉一次板块表
		enqueue(boardList);
		assertThat(client().boardCodes()).containsEntry("电池", "BK0479");
		enqueue(boardList);
		enqueue("""
				{"data":{"klines":["2026-09-30,1.0,2.0,2.5,0.9,100,200.0"]}}""");
		List<KlineBar> bars = client().boardKline("银行", "20260901", "20260930");
		assertThat(bars).hasSize(1);
		assertThat(bars.get(0).code()).isEqualTo("银行");
		assertThat(bars.get(0).close()).isEqualByComparingTo(BigDecimal.valueOf(2));
	}

}
