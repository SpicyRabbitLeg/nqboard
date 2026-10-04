package com.mx.nqboard.sniper.data.provider.em;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.data.RequestAuditSink;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.NewsItem;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.mx.nqboard.sniper.data.provider.support.CircuitBreaker;
import com.mx.nqboard.sniper.data.provider.support.ThrottledHttpClient;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 东方财富直连客户端（照 Python web_fallback.py + akshare 1.18.64 底层逐端点核对）。
 * 全部请求带浏览器 UA + Referer + Connection: close（缺 UA 会触发 RemoteDisconnected）。
 * </p>
 * <ul>
 * <li>K线：push2his kline/get，CSV 行序为 <b>日期,开盘,收盘,最高,最低,…（O-C-H-L，非 O-H-L-C）</b>；
 * fields2/ut 照 akshare 1.18.64（f51..f61+f116，ut 固定 token）</li>
 * <li>快照：clist 分页 pz=100，URL/参数照 akshare 1.18.64（82.push2 镜像、fid=f12、total 求总页数、
 * 合并后按 f3 降序——与 Python 请求序列一致，对拍 fixtures 基准）</li>
 * <li>板块代码表：clist fs=m:90+t:2，push2→push2delay 双 host，翻页 ≤6 页</li>
 * <li>f127 行业：push2delay→push2，过滤 "-"/"--"/"nan"</li>
 * <li>新闻：search-api-web jsonp——akshare stock_news_em 底层与本兜底<b>同源同接口</b>，单实现两用</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class EastmoneyClient {

	/** akshare 1.18.64 kline 的 ut 固定 token（防爬参数，缺失可能被拒） */
	static final String UT_KLINE = "7eea3edcaed734bea9cbfc24409ed989";

	/** akshare 1.18.64 clist 的 ut 固定 token */
	static final String UT_CLIST = "bd1d9ddb04089700cf9c27f6f7426281";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final ThrottledHttpClient http;

	private final String push2hisHost;

	private final String push2Host;

	private final String push2DelayHost;

	private final String clistHost;

	private final String searchHost;

	public EastmoneyClient(CircuitBreaker circuitBreaker, RequestAuditSink auditSink) {
		this(circuitBreaker, auditSink, "https://push2his.eastmoney.com", "https://push2.eastmoney.com",
				"https://push2delay.eastmoney.com", "https://82.push2.eastmoney.com",
				"https://search-api-web.eastmoney.com");
	}

	/** 包内测试构造：五类 host 可覆盖为内嵌 server（生产默认真实域名） */
	public EastmoneyClient(CircuitBreaker circuitBreaker, RequestAuditSink auditSink, String push2hisHost,
			String push2Host,
			String push2DelayHost, String clistHost, String searchHost) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("User-Agent",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
						+ "Chrome/124.0.0.0 Safari/537.36");
		headers.put("Referer", "https://quote.eastmoney.com/");
		// 注：Python 版另设 Connection: close 防 stale keep-alive（RemoteDisconnected），
		// JDK HttpClient 将该头列为受限头不可设置，由其内置 stale 连接自动重试语义兜底
		this.http = new ThrottledHttpClient("em", ThrottledHttpClient.DEFAULT_MIN_DELAY,
				ThrottledHttpClient.DEFAULT_BACKOFF_SECONDS, ThrottledHttpClient.DEFAULT_HTTP_TIMEOUT,
				circuitBreaker, auditSink, headers);
		this.push2hisHost = push2hisHost;
		this.push2Host = push2Host;
		this.push2DelayHost = push2DelayHost;
		this.clistHost = clistHost;
		this.searchHost = searchHost;
	}

	/**
	 * 个股日 K 线（fqt：0=不复权 1=qfq 2=hfq；klines CSV 行序 O-C-H-L）。
	 */
	public List<KlineBar> stockKline(String code, String fqt, String beg, String end) {
		int market = code.startsWith("6") || code.startsWith("9") || code.startsWith("5") ? 1 : 0;
		Map<String, String> params = new LinkedHashMap<>();
		params.put("secid", market + "." + code);
		params.put("fields1", "f1,f2,f3,f4,f5,f6");
		params.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f116");
		params.put("ut", UT_KLINE);
		params.put("klt", "101");
		params.put("fqt", fqt);
		params.put("beg", beg);
		params.put("end", end);
		String body = http.get(push2hisHost + "/api/qt/stock/kline/get", params, null, 3);
		return parseKlines(code, body, "em");
	}

	/**
	 * 行业板块日 K 线（fqt=0；板块名→BK 码先精确后互为子串模糊匹配）。
	 */
	public List<KlineBar> boardKline(String boardName, String beg, String end) {
		String bk = resolveBoardCode(boardName);
		if (bk == null) {
			log.warn("em board kline: board code not found for {}", boardName);
			return List.of();
		}
		Map<String, String> params = new LinkedHashMap<>();
		params.put("secid", "90." + bk);
		params.put("fields1", "f1,f2,f3,f4,f5,f6");
		params.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61");
		params.put("ut", UT_KLINE);
		params.put("klt", "101");
		params.put("fqt", "0");
		params.put("beg", beg);
		params.put("end", end);
		String body = http.get(push2hisHost + "/api/qt/stock/kline/get", params, null, 2);
		return parseKlines(boardName, body, "em");
	}

	/**
	 * 板块名 → BK 码映射（clist fs=m:90+t:2，push2→push2delay 双 host，翻页 ≤6 页）。
	 * 缓存由上层（RedisUtils 长缓存）负责，本方法每次现拉。
	 */
	public Map<String, String> boardCodes() {
		for (String host : new String[] { push2Host, push2DelayHost }) {
			try {
				Map<String, String> codes = fetchBoardCodes(host);
				if (!codes.isEmpty()) {
					return codes;
				}
			}
			catch (RuntimeException e) {
				log.warn("em board list failed on {}: {}", host, e.getMessage());
			}
		}
		return Map.of();
	}

	private Map<String, String> fetchBoardCodes(String host) {
		Map<String, String> codes = new LinkedHashMap<>();
		int page = 1;
		while (page <= 6) {
			Map<String, String> params = new LinkedHashMap<>();
			params.put("pn", String.valueOf(page));
			params.put("pz", "100");
			params.put("po", "1");
			params.put("np", "1");
			params.put("fltt", "2");
			params.put("invt", "2");
			params.put("fid", "f3");
			params.put("fs", "m:90+t:2");
			params.put("fields", "f12,f14");
			JsonNode diff = clistDiff(http.get(host + "/api/qt/clist/get", params, null, 2));
			if (diff == null || !diff.isArray() || diff.isEmpty()) {
				break;
			}
			for (JsonNode item : diff) {
				String name = item.path("f14").asText("").trim();
				String code = item.path("f12").asText("").trim();
				if (!name.isEmpty() && !code.isEmpty()) {
					codes.put(name, code);
				}
			}
			if (diff.size() < 100) {
				break;
			}
			page++;
		}
		return codes;
	}

	private String resolveBoardCode(String boardName) {
		Map<String, String> boards = boardCodes();
		String bk = boards.get(boardName);
		if (bk != null) {
			return bk;
		}
		// 模糊匹配：互为子串（照 akshare_client._resolve_industry_board_name 语义）
		for (Map.Entry<String, String> entry : boards.entrySet()) {
			if (boardName.contains(entry.getKey()) || entry.getKey().contains(boardName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 个股行业（f127，EM 原生口径与板块名对齐；push2delay 优先、push2 兜底）。
	 */
	public String industryByCode(String code) {
		int market = code.startsWith("6") || code.startsWith("9") || code.startsWith("5") ? 1 : 0;
		for (String host : new String[] { push2DelayHost, push2Host }) {
			try {
				Map<String, String> params = new LinkedHashMap<>();
				params.put("fltt", "2");
				params.put("invt", "2");
				params.put("fields", "f57,f58,f127");
				params.put("secid", market + "." + code);
				JsonNode data = MAPPER.readTree(http.get(host + "/api/qt/stock/get", params, null, 2)).path("data");
				String raw = data.path("f127").asText("").trim();
				if (!raw.isEmpty() && !raw.equals("-") && !raw.equals("--") && !raw.equals("nan")) {
					return raw;
				}
			}
			catch (RuntimeException | java.io.IOException e) {
				log.warn("em industry fetch failed on {} for {}: {}", host, code, e.getMessage());
			}
		}
		return null;
	}

	/**
	 * 全市场快照（clist 分页 pz=100，URL/参数/翻页照 akshare 1.18.64：82.push2 镜像、
	 * fid=f12、data.total 求总页数；合并后按 f3 降序对齐 Python 结果顺序）。
	 */
	public List<QuoteSnapshot> spotAll() {
		List<JsonNode> rows = new ArrayList<>();
		ClistPage first = clistPage(spotParams(1));
		if (first.diff().isEmpty()) {
			return List.of();
		}
		rows.addAll(first.diff());
		int totalPage = (int) Math.ceil((double) first.total() / Math.max(first.diff().size(), 1));
		log.info("em spot clist: {} rows on page 1, total={}, pages={}", first.diff().size(), first.total(),
				totalPage);
		for (int page = 2; page <= totalPage; page++) {
			rows.addAll(clistPage(spotParams(page)).diff());
		}
		List<QuoteSnapshot> snapshots = new ArrayList<>();
		for (JsonNode item : rows) {
			snapshots.add(new QuoteSnapshot(item.path("f12").asText("").trim(), item.path("f14").asText(""),
					numeric(item.get("f2")), numeric(item.get("f3")), numeric(item.get("f17")),
					numeric(item.get("f15")), numeric(item.get("f16")), numeric(item.get("f18")),
					numeric(item.get("f5")), numeric(item.get("f6")), numeric(item.get("f8")), "em"));
		}
		// 照 akshare：合并后按涨跌幅(f3)降序，null 沉底
		snapshots.sort((a, b) -> {
			if (a.changePct() == null && b.changePct() == null) {
				return 0;
			}
			if (a.changePct() == null) {
				return 1;
			}
			if (b.changePct() == null) {
				return -1;
			}
			return b.changePct().compareTo(a.changePct());
		});
		return snapshots;
	}

	private String spotParams(int page) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("pn", String.valueOf(page));
		params.put("pz", "100");
		params.put("po", "1");
		params.put("np", "1");
		params.put("ut", UT_CLIST);
		params.put("fltt", "2");
		params.put("invt", "2");
		params.put("fid", "f12");
		params.put("fs", "m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048");
		params.put("fields",
				"f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,"
						+ "f62,f128,f136,f115,f152");
		return http.get(clistHost + "/api/qt/clist/get", params, null, 3);
	}

	/**
	 * 个股新闻（search-api-web jsonp：主源与兜底同源；keyword 用 6 位代码非名称）。
	 * cb 固定 jQuerycb 剥壳（照 web_fallback.search_news_em），pageSize=max(limit,20)。
	 */
	public List<NewsItem> searchNews(String keyword, int limit) {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("uid", "");
		inner.put("keyword", keyword);
		inner.put("type", List.of("cmsArticleWebOld"));
		inner.put("client", "web");
		inner.put("clientType", "web");
		inner.put("clientVersion", "curr");
		Map<String, Object> articleParam = new LinkedHashMap<>();
		articleParam.put("searchScope", "default");
		articleParam.put("sort", "default");
		articleParam.put("pageIndex", 1);
		articleParam.put("pageSize", Math.max(limit, 20));
		articleParam.put("preTag", "<em>");
		articleParam.put("postTag", "</em>");
		inner.put("param", articleParam);
		String paramJson;
		try {
			paramJson = MAPPER.writeValueAsString(inner);
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException("em news param serialize failed", e);
		}
		Map<String, String> params = new LinkedHashMap<>();
		params.put("cb", "jQuerycb");
		params.put("param", paramJson);
		String text = http.get(searchHost + "/search/jsonp", params, null, 2).trim();
		String payload = text.replaceAll("^jQuerycb\\(|\\)$", "");
		JsonNode articles;
		try {
			articles = MAPPER.readTree(payload).path("result").path("cmsArticleWebOld");
		}
		catch (java.io.IOException e) {
			throw new com.mx.nqboard.sniper.data.provider.support.ThrottledHttpClient.HttpStatusException(
					"em news bad json: " + keyword, 0);
		}
		List<NewsItem> items = new ArrayList<>();
		if (!articles.isArray()) {
			return items;
		}
		for (JsonNode art : articles) {
			if (items.size() >= limit) {
				break;
			}
			String title = stripTags(art.path("title").asText(""));
			String content = stripTags(art.path("content").asText(""));
			String date = art.path("date").asText("");
			String mediaName = art.path("mediaName").asText("");
			items.add(new NewsItem(title, date.length() > 10 ? date.substring(0, 10) : date,
					art.path("url").asText(""), mediaName.isEmpty() ? "东方财富" : mediaName, content));
		}
		return items;
	}

	// ------------------------------------------------------------------
	// 解析私有段
	// ------------------------------------------------------------------

	private JsonNode clistDiff(String body) {
		try {
			return MAPPER.readTree(body).path("data").path("diff");
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException("em clist bad json", e);
		}
	}

	private record ClistPage(List<JsonNode> diff, int total) {
	}

	private ClistPage clistPage(String body) {
		JsonNode diff = clistDiff(body);
		List<JsonNode> rows = new ArrayList<>();
		if (diff.isArray()) {
			diff.forEach(rows::add);
		}
		int total;
		try {
			total = MAPPER.readTree(body).path("data").path("total").asInt(0);
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException("em clist bad json", e);
		}
		return new ClistPage(rows, total);
	}

	private List<KlineBar> parseKlines(String code, String body, String source) {
		JsonNode klines;
		try {
			klines = MAPPER.readTree(body).path("data").path("klines");
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException("em kline bad json for " + code, e);
		}
		List<KlineBar> bars = new ArrayList<>();
		if (!klines.isArray()) {
			return bars;
		}
		for (JsonNode line : klines) {
			String[] parts = line.asText().split(",");
			if (parts.length < 7) {
				continue;
			}
			// CSV 行序：日期,开盘,收盘,最高,最低,成交量,成交额,振幅,涨跌幅,涨跌额,换手率 —— O-C-H-L！
			bars.add(new KlineBar(code, LocalDate.parse(parts[0]), num(parts[1]), num(parts[3]), num(parts[4]),
					num(parts[2]), num(parts[5]), num(parts[6]), null, source));
		}
		return bars;
	}

	private BigDecimal num(String text) {
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

	private BigDecimal numeric(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		String text = node.asText();
		return num(text);
	}

	private String stripTags(String text) {
		return text.replaceAll("</?em>", "");
	}

}
