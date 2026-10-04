package com.mx.nqboard.sniper.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 数据级对拍第一层：fixtures 契约覆盖（主文档 §12 数据级）——Python 版 2026-10-04 录制的
 * 请求日志（as_of=2026-09-30，规则模式+市场门关闭，1641 scan + 58 track = 1699 条）中出现的
 * 全部 (source, endpoint) 组合必须落在 Java 数据接入的已实现契约清单内；
 * golden master（scan_2026-09-30.json）顶层 schema 完整（M5 报告级对拍的输入前置校验）。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
class FixtureContractTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String FIXTURE_LOG = "/fixtures/cn_request_log_20261004.jsonl";

	private static final String GOLDEN_MASTER = "/fixtures/scan_2026-09-30.json";

	private static final Set<String> COVERED_COMBOS = Set.of(
			// Tushare（TushareClient：pro_bar=daily+adj_factor 合成；tushare_api=daily_basic/
			// fina_indicator/trade_cal/stock_basic/index_weight/index_daily 等）
			"tushare|prices",
			"tushare|tushare.pro_bar",
			"tushare|tushare.tushare_api",
			"tushare|get_insider_trades",
			"tushare|get_stock_industry",
			"tushare|get_main_fund_flow",
			"tushare|get_dragon_tiger",
			"tushare|get_financial_metrics",
			"tushare|get_restricted_release",
			// web_fallback 直连（EastmoneyClient：f127 行业、板块表+板块K线）
			"em-direct|get_stock_industry",
			"http|http.get",
			// akshare 层（Java 对应实现：news=search jsonp、board=板块K线、
			// northbound=停发恒空、spot=clist 快照、financial=腿1+腿2）
			"akshare|get_company_news",
			"akshare|get_insider_trades",
			"akshare|get_northbound_holdings",
			"akshare|get_industry_board_hist",
			"akshare|get_financial_metrics",
			"akshare|get_dragon_tiger",
			"akshare|get_restricted_release",
			"akshare-em|spot.full",
			"akshare-sina|spot.full",
			"akshare-sina|spot.normalized",
			// composite 路由层（Java CompositeProvider _routed 语义）
			"routed|get_stock_industry");

	private static int recordCount;

	private static List<JsonNode> records;

	@BeforeAll
	static void loadFixture() throws Exception {
		try (var in = FixtureContractTest.class.getResourceAsStream(FIXTURE_LOG)) {
			assertThat(in).as("fixtures 请求日志必须落位测试资源").isNotNull();
			// JSONL：每行一个对象，逐行解析
			records = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
				.lines()
				.filter(l -> !l.isBlank())
				.map(l -> {
					try {
						return MAPPER.readTree(l);
					}
					catch (java.io.IOException e) {
						throw new IllegalStateException("bad fixture line", e);
					}
				})
				.toList();
			recordCount = records.size();
		}
	}

	@Test
	@DisplayName("fixtures 规模：与 Python 录制一致的 1699 条请求")
	void fixtureRecordCount() {
		assertThat(recordCount).isEqualTo(1699);
	}

	@Test
	@DisplayName("契约覆盖：fixtures 出现的全部 (source, endpoint) 组合都有 Java 实现")
	void allFixtureEndpointsHaveJavaCounterpart() {
		Set<String> seen = new java.util.HashSet<>();
		for (JsonNode rec : records) {
			seen.add(rec.path("source").asText() + "|" + rec.path("endpoint").asText());
		}
		Set<String> uncovered = new java.util.HashSet<>(seen);
		uncovered.removeAll(COVERED_COMBOS);
		assertThat(uncovered).as("Python 录制中出现但 Java 未实现的端点组合").isEmpty();
	}

	@Test
	@DisplayName("fixtures 含真实降级样本（error/empty_fallback），契约测试不回避失败路径")
	void fixtureContainsRealDegradation() {
		int error = 0;
		int emptyFallback = 0;
		for (JsonNode rec : records) {
			switch (rec.path("status").asText()) {
				case "error" -> error++;
				case "empty_fallback" -> emptyFallback++;
			}
		}
		assertThat(error).as("录制含东财限流等真实 error 样本").isGreaterThan(0);
		assertThat(emptyFallback).as("录制含 tushare 空结果降级样本").isGreaterThan(0);
	}

	@Test
	@DisplayName("golden master：scan JSON 顶层 schema 完整（§10.1）")
	void goldenMasterSchema() throws Exception {
		try (var in = FixtureContractTest.class.getResourceAsStream(GOLDEN_MASTER)) {
			assertThat(in).as("golden master 必须落位测试资源").isNotNull();
			JsonNode root = MAPPER.readTree(in);
			// §10.1 顶层字段（market gate 关闭录制：blocked=false，meta 带市场环境）
			assertThat(root.path("mode").asText()).isEqualTo("flat_short");
			assertThat(root.path("gate_version").asText()).isEqualTo("3");
			assertThat(root.path("as_of").asText()).isEqualTo("2026-09-30");
			assertThat(root.path("market_ret_5d").isNumber()).isTrue();
			assertThat(root.has("signals")).isTrue();
			assertThat(root.has("rejected")).isTrue();
			assertThat(root.has("meta")).isTrue();
			JsonNode meta = root.path("meta");
			assertThat(meta.has("budget")).as("meta.budget 预算统计").isTrue();
			// 市场门关闭录制：signals 可为空但字段在；rejected 含被刷票
			assertThat(root.path("market_gate_blocked").asBoolean()).isFalse();
		}
	}

}
