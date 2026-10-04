package com.mx.nqboard.sniper.data.provider;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.provider.em.EastmoneyClient;
import com.mx.nqboard.sniper.data.provider.sina.SinaClient;
import com.mx.nqboard.sniper.data.provider.tencent.TencentClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CompositeProvider 路由语义单测——4 级行情链、qfq 禁降新浪、反向兜底、快照预算无条件计 1、qfq 合成。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class CompositeProviderTest {

	private HttpServer server;

	/** 全部源 URL 指向内嵌 server；响应按请求顺序出队 */
	private final List<String> bodyQueue = new ArrayList<>();

	private final AtomicInteger tushareHits = new AtomicInteger();

	private final AtomicInteger emHits = new AtomicInteger();

	private final AtomicInteger tencentHits = new AtomicInteger();

	private final AtomicInteger sinaHits = new AtomicInteger();

	private final AtomicReference<String> lastEmQuery = new AtomicReference<>("");

	private final AtomicReference<String> lastTencentQuery = new AtomicReference<>("");

	@BeforeEach
	void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		tushareHits.set(0);
		emHits.set(0);
		tencentHits.set(0);
		sinaHits.set(0);
		server.createContext("/", exchange -> {
			String path = exchange.getRequestURI().getPath();
			if (path.startsWith("/tushare")) {
				tushareHits.incrementAndGet();
			}
			else if (path.startsWith("/em")) {
				emHits.incrementAndGet();
				lastEmQuery.set(exchange.getRequestURI().getRawQuery());
			}
			else if (path.startsWith("/tencent")) {
				tencentHits.incrementAndGet();
				lastTencentQuery.set(exchange.getRequestURI().getRawQuery());
			}
			else if (path.startsWith("/sina")) {
				sinaHits.incrementAndGet();
			}
			String body = bodyQueue.isEmpty() ? "{}" : bodyQueue.remove(0);
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
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

	private static final String EMPTY_TUSHARE = "{\"code\":0,\"msg\":null,\"data\":{\"fields\":[\"ts_code\"],"
			+ "\"items\":[],\"has_more\":false}}";

	private static final String EMPTY_EM = "{\"data\":{\"klines\":[]}}";

	private static final String EMPTY_TENCENT = "{\"code\":0,\"data\":{\"sh600519\":{}}}";

	private static final String SINA_2BARS = """
			[{"day":"2026-09-29","open":"10.0","high":"10.5","low":"9.9","close":"10.2","volume":"100000"},
			 {"day":"2026-09-30","open":"10.2","high":"10.8","low":"10.1","close":"10.6","volume":"200000"}]""";

	/** 全部源指到内嵌 server 的 CompositeProvider */
	private CompositeProvider provider(boolean tusharePreferredForPrices, boolean tushareFirst,
			boolean priceFallbackTushare, DailyBudget budget) {
		String base = "http://127.0.0.1:" + server.getAddress().getPort();
		TushareClient tushare = new TushareClient("test-token", java.time.Duration.ofMillis(20), 1, 0.01,
				java.time.Duration.ofSeconds(2), URI.create(base + "/tushare"), null);
		EastmoneyClient em = new EastmoneyClient(null, null, base + "/em", base + "/em", base + "/em",
				base + "/em", base + "/em");
		TencentClient tencent = new TencentClient(null, base + "/tencent/fqkline", base + "/tencent/q=");
		SinaClient sina = new SinaClient(null, base + "/sina/kline", base + "/sina/list=", base + "/sina/spot");
		return new CompositeProvider(tushare, em, tencent, sina, budget, tusharePreferredForPrices, tushareFirst,
				priceFallbackTushare);
	}

	@Test
	@DisplayName("Tushare 优先成功：qfq 借库内 max_factor 合成，EM/腾讯/新浪零请求")
	void tusharePreferredServesQfq() {
		// daily 行（含 pre_close/vol/amount） + 窗口因子行
		enqueue("{\"code\":0,\"msg\":null,\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"open\",\"high\",\"low\","
				+ "\"close\",\"vol\",\"amount\",\"pre_close\"],"
				+ "\"items\":[[\"600519.SH\",\"20260930\",\"10\",\"10.5\",\"9.8\",\"10.2\",\"12345\",\"5678\",\"10.0\"]],"
				+ "\"has_more\":false}}");
		enqueue("{\"code\":0,\"msg\":null,\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"adj_factor\"],"
				+ "\"items\":[[\"600519.SH\",\"20260930\",\"10.0\"]],\"has_more\":false}}");

		// maxFactor=10.0 → qfq = none × 10 ÷ 10 = none（便于断言）
		List<KlineBar> bars = provider(true, true, true, new DailyBudget(300)).prices("600519", "SH",
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30), AdjustEnum.QFQ, code -> BigDecimal.TEN);

		assertThat(bars).hasSize(1);
		assertThat(bars.get(0).close()).isEqualByComparingTo("10.2");
		assertThat(bars.get(0).source()).isEqualTo("tushare");
		assertThat(bars.get(0).preClose()).isEqualByComparingTo("10.0");
		// 复权只作用价格：volume/amount 原样
		assertThat(bars.get(0).volume()).isEqualByComparingTo("12345");
		assertThat(bars.get(0).amount()).isEqualByComparingTo("5678");
		assertThat(emHits.get()).isZero();
		assertThat(sinaHits.get()).isZero();
	}

	@Test
	@DisplayName("Tushare 空且无 maxFactor 依赖时 qfq 直接由 EM fqt=1 接管（跳过 Tushare 级，不发 Tushare 请求）")
	void qfqFallsBackToEmWhenNoMaxFactor() {
		enqueue("{\"data\":{\"klines\":[\"2026-09-30,10.0,11.0,11.5,9.8,12345,123456.0\"]}}");

		List<KlineBar> bars = provider(true, true, true, new DailyBudget(300)).prices("600519", "SH",
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30), AdjustEnum.QFQ, null);

		assertThat(bars).hasSize(1);
		assertThat(bars.get(0).close()).isEqualByComparingTo("11.0");
		assertThat(lastEmQuery.get()).contains("fqt=1");
		assertThat(tushareHits.get()).as("无合成依赖时 qfq 不发 Tushare 请求").isZero();
	}

	@Test
	@DisplayName("qfq 请求禁降新浪（三级耗尽返回空）；none 请求新浪接管（股→手）")
	void qfqNeverFallsToSina() {
		// qfq（非 prefer 链：EM→腾讯→新浪跳过）
		enqueue(EMPTY_EM);
		enqueue(EMPTY_TENCENT);
		List<KlineBar> qfqBars = provider(false, true, false, new DailyBudget(10)).prices("600519", "SH",
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30), AdjustEnum.QFQ, null);
		assertThat(qfqBars).isEmpty();
		assertThat(sinaHits.get()).as("qfq 请求不得降级新浪").isZero();

		// none：第 3 级新浪接管（非 prefer 链同样从 EM 起）
		enqueue(EMPTY_EM);
		enqueue(EMPTY_TENCENT);
		enqueue(SINA_2BARS);
		List<KlineBar> noneBars = provider(false, true, false, new DailyBudget(10)).prices("600519", "SH",
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30), AdjustEnum.NONE, null);
		// 窗口单日：新浪两行中 9-29 被窗口过滤，仅 9-30 通过
		assertThat(noneBars).hasSize(1);
		// 新浪股→手：200000 股 → 2000 手
		assertThat(noneBars.get(0).volume()).isEqualByComparingTo("2000");
		assertThat(noneBars.get(0).source()).isEqualTo("sina");
	}

	@Test
	@DisplayName("反向兜底：非 Tushare 优先三级全空按 CN_PRICE_FALLBACK 回退 Tushare")
	void priceFallbackToTushare() {
		// 非 prefer none 链：EM 空 → 腾讯空 → 新浪空 → 反向兜底 Tushare none 行
		enqueue(EMPTY_EM);
		enqueue(EMPTY_TENCENT);
		enqueue("[]");
		enqueue("{\"code\":0,\"msg\":null,\"data\":{\"fields\":[\"ts_code\",\"trade_date\",\"open\",\"high\",\"low\","
				+ "\"close\",\"vol\",\"amount\",\"pre_close\"],"
				+ "\"items\":[[\"600519.SH\",\"20260930\",\"10\",\"10.5\",\"9.8\",\"10.2\",\"12345\",\"5678\",\"10.0\"]],"
				+ "\"has_more\":false}}");
		List<KlineBar> bars = provider(false, true, true, new DailyBudget(10)).prices("600519", "SH",
				LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30), AdjustEnum.NONE, null);

		assertThat(bars).hasSize(1);
		assertThat(bars.get(0).source()).isEqualTo("tushare");
		assertThat(tushareHits.get()).isGreaterThan(0);
	}

	@Test
	@DisplayName("预算：快照无条件消耗 1；东财主源模式行情请求不消耗预算")
	void budgetSemantics() {
		// 快照无条件 1：EM 空 + 新浪空 + 腾讯空（无代码表）
		DailyBudget budget = new DailyBudget(5);
		enqueue(EMPTY_EM);
		enqueue("[]");
		provider(false, true, false, budget).spot(List.of());
		assertThat(budget.used()).as("快照无条件计 1").isEqualTo(1);

		// 东财主源模式（tusharePreferred）：行情链（tushare→EM→腾讯→新浪 全走一遍）不消耗预算
		DailyBudget preferBudget = new DailyBudget(5);
		enqueue(EMPTY_TUSHARE);
		enqueue(EMPTY_EM);
		enqueue(EMPTY_TENCENT);
		enqueue(SINA_2BARS);
		provider(true, true, true, preferBudget).prices("600519", "SH", LocalDate.of(2026, 9, 30),
				LocalDate.of(2026, 9, 30), AdjustEnum.NONE, null);
		assertThat(preferBudget.used()).as("tushare 优先模式不消耗预算").isZero();
	}

	@Test
	@DisplayName("快照降级链：东财 clist 空 → 新浪行情中心接管")
	void spotFallsToSinaMarketCenter() {
		DailyBudget budget = new DailyBudget(300);
		enqueue(EMPTY_EM);
		enqueue("""
				[{"code":"600519","name":"贵州茅台","trade":"1800.0","settlement":"1770.0","amount":"3600000000",
				  "volume":"2000000","open":"1790.0","high":"1810.0","low":"1780.0"}]""");

		List<com.mx.nqboard.sniper.data.model.QuoteSnapshot> snapshots = provider(false, true, false, budget)
			.spot(List.of("sh600519"));

		assertThat(snapshots).hasSize(1);
		assertThat(snapshots.get(0).code()).isEqualTo("600519");
		assertThat(budget.used()).isEqualTo(1);
	}

	@Test
	@DisplayName("行业：f127 EM 直连优先，失败降 Tushare 旧口径兜底")
	void industryPrefersEmDirect() {
		enqueue("{\"data\":{\"f57\":\"600519\",\"f58\":\"贵州茅台\",\"f127\":\"电池\"}}");
		assertThat(provider(true, true, true, new DailyBudget(300)).industry("600519",
				code -> "电气设备")).isEqualTo("电池");

		enqueue("{\"data\":{\"f127\":\"--\"}}");
		assertThat(provider(true, true, true, new DailyBudget(300)).industry("600519", code -> "电气设备"))
			.isEqualTo("电气设备");
	}

}
