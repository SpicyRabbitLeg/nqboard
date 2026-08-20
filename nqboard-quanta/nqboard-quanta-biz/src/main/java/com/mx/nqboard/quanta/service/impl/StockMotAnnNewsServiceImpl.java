package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockMotAnnNewsEntity;
import com.mx.nqboard.quanta.mapper.StockMotAnnNewsMapper;
import com.mx.nqboard.quanta.service.StockMotAnnNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 公告&媒体新闻表 服务实现类
 * </p>
 * <p>
 * 数据来源：东方财富（个股公告 np-anotice-stock + 媒体新闻 search-api-web），
 * 东方财富连续失败 {@link #DEGRADE_THRESHOLD} 次后自动降级到巨潮资讯（cninfo 公告）。
 * 仅提供单股票同步接口（外部手动调用），不做全市场定时任务（防 IP 被封）。
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMotAnnNewsServiceImpl extends ServiceImpl<StockMotAnnNewsMapper, StockMotAnnNewsEntity>
		implements StockMotAnnNewsService {

	/**
	 * 东方财富个股公告接口
	 */
	private static final String EF_ANN_URL = "https://np-anotice-stock.eastmoney.com/api/security/ann";

	/**
	 * 东方财富个股媒体新闻搜索接口（jsonp）
	 */
	private static final String EF_NEWS_URL = "https://search-api-web.eastmoney.com/search/jsonp";

	/**
	 * 巨潮资讯公告查询接口
	 */
	private static final String CNINFO_URL = "http://www.cninfo.com.cn/new/hisAnnouncement/query";

	/**
	 * 巨潮资讯代码→orgId 解析接口（POST form）
	 */
	private static final String CNINFO_SEARCH_URL = "http://www.cninfo.com.cn/new/information/topSearch/query";

	/**
	 * 巨潮资讯公告原文前缀
	 */
	private static final String CNINFO_FILE_PREFIX = "http://static.cninfo.com.cn";

	/**
	 * 东方财富公告详情页前缀
	 */
	private static final String EF_ANN_DETAIL_PREFIX = "https://data.eastmoney.com/notices/detail/";

	/**
	 * 巨潮资讯请求 User-Agent
	 */
	private static final String CNINFO_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

	/**
	 * 全量同步起始日期：2024-01-01
	 */
	private static final LocalDate FULL_SYNC_START = LocalDate.of(2024, 1, 1);

	/**
	 * 东方财富连续失败降级阈值：连续失败达到该次数后自动降级到巨潮资讯（防止 IP 被封影响主链路）
	 */
	private static final int DEGRADE_THRESHOLD = 3;

	/**
	 * 东方财富公告分页参数
	 */
	private static final int EF_PAGE_SIZE = 50;
	private static final int EF_MAX_PAGES = 5;

	/**
	 * 东方财富媒体新闻分页参数
	 */
	private static final int EF_NEWS_PAGE_SIZE = 50;
	private static final int EF_NEWS_MAX_PAGES = 3;

	/**
	 * 巨潮资讯分页参数
	 */
	private static final int CNINFO_PAGE_SIZE = 30;
	private static final int CNINFO_MAX_PAGES = 10;

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter CNINFO_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

	private final StockMotAnnNewsMapper stockMotAnnNewsMapper;

	/**
	 * 复用日线全量同步开关（yml 配置 tushare.daily.full）
	 */
	@Value("${tushare.daily.full:false}")
	private boolean syncFull;

	/**
	 * 东方财富连续失败计数（进程内状态，重启后归零）
	 */
	private int efFailCount = 0;

	/**
	 * 是否已降级到巨潮资讯：降级后保持，直到服务重启
	 */
	private volatile boolean degradedToCninfo = false;

	/**
	 * 按股票代码同步公告&媒体新闻（全量/增量取 yml 配置 tushare.daily.full）
	 */
	@Override
	public int syncNews(String tsCode) {
		return syncNews(tsCode, null);
	}

	/**
	 * 按股票代码同步公告&媒体新闻（东方财富失败自动降级巨潮资讯）
	 */
	@Override
	public int syncNews(String tsCode, Boolean full) {
		if (StrUtil.isBlank(tsCode)) {
			throw new IllegalArgumentException("tsCode 不能为空");
		}
		boolean syncFull = full != null ? full : this.syncFull;
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = syncFull ? FULL_SYNC_START : endDate;
		log.info("开始同步 {} 的公告&新闻, full={}, 区间: {} ~ {}", tsCode, syncFull, startDate, endDate);

		// 1. 优先东方财富（未降级时）
		if (!degradedToCninfo) {
			try {
				List<StockMotAnnNewsEntity> rows = fetchEastMoney(tsCode, startDate, endDate);
				efFailCount = 0;
				int affected = upsertByUniqueKey(rows);
				log.info("{} 东方财富同步成功, 影响 {} 条", tsCode, affected);
				return affected;
			}
			catch (Exception e) {
				efFailCount++;
				log.error("{} 东方财富同步失败(第 {}/{} 次): {}", tsCode, efFailCount, DEGRADE_THRESHOLD, e.getMessage());
				if (efFailCount >= DEGRADE_THRESHOLD) {
					degradedToCninfo = true;
					log.warn("东方财富连续失败 {} 次，自动降级到巨潮资讯（服务重启前保持降级）", efFailCount);
				}
			}
		}

		// 2. 降级到巨潮资讯（公告）
		try {
			List<StockMotAnnNewsEntity> rows = fetchCninfo(tsCode, startDate, endDate);
			int affected = upsertByUniqueKey(rows);
			log.info("{} 巨潮资讯同步成功, 影响 {} 条", tsCode, affected);
			return affected;
		}
		catch (Exception e) {
			log.error("{} 巨潮资讯同步失败: {}", tsCode, e.getMessage());
			return 0;
		}
	}

	/**
	 * 东方财富：个股公告 + 媒体新闻
	 */
	private List<StockMotAnnNewsEntity> fetchEastMoney(String tsCode, LocalDate startDate, LocalDate endDate) {
		String code = tsCode.split("\\.")[0];
		List<StockMotAnnNewsEntity> list = new ArrayList<>();
		// 1.1 个股公告（按公告日期倒序分页）
		outer:
		for (int page = 1; page <= EF_MAX_PAGES; page++) {
			Map<String, Object> params = new HashMap<>(8);
			params.put("sr", -1);
			params.put("page_size", EF_PAGE_SIZE);
			params.put("page_index", page);
			params.put("ann_type", "A");
			params.put("client_source", "web");
			params.put("stock_list", code);
			params.put("f_node", 0);
			params.put("s_node", 0);
			JSONObject data = getJson(EF_ANN_URL, params).getJSONObject("data");
			JSONArray items = data == null ? null : data.getJSONArray("list");
			if (items == null || items.isEmpty()) {
				break;
			}
			for (int i = 0; i < items.size(); i++) {
				JSONObject it = items.getJSONObject(i);
				// notice_date 为字符串，如 "2026-08-14 00:00:00"
				String noticeDate = it.getString("notice_date");
				LocalDate pub = parseDateStr(noticeDate);
				// 列表按日期倒序，遇到早于起始日期的直接结束
				if (pub == null || pub.isBefore(startDate)) {
					break outer;
				}
				if (pub.isAfter(endDate)) {
					continue;
				}
				StockMotAnnNewsEntity entity = new StockMotAnnNewsEntity();
				entity.setTsCode(tsCode);
				entity.setPubDate(pub.format(DATE_FMT));
				entity.setPubDatetime(noticeDate);
				entity.setNewsType("ann");
				entity.setSrc("东方财富");
				entity.setTitle(clean(it.getString("title")));
				entity.setSummary(clean(it.getString("summary")));
				entity.setUrl(EF_ANN_DETAIL_PREFIX + code + "/" + it.getString("art_code") + ".html");
				list.add(entity);
			}
			if (items.size() < EF_PAGE_SIZE) {
				break;
			}
		}
		// 1.2 个股媒体新闻（搜索接口，按发布时间倒序分页）
		outer:
		for (int page = 1; page <= EF_NEWS_MAX_PAGES; page++) {
			JSONObject param = new JSONObject();
			param.put("uid", "");
			param.put("keyword", code);
			param.put("type", List.of("cmsArticleWebOld"));
			param.put("client", "web");
			param.put("clientType", "web");
			param.put("clientVersion", "curr");
			JSONObject inner = new JSONObject();
			JSONObject search = new JSONObject();
			search.put("searchScope", "default");
			search.put("sort", "default");
			search.put("pageIndex", page);
			search.put("pageSize", EF_NEWS_PAGE_SIZE);
			search.put("preTag", "<em>");
			search.put("postTag", "</em>");
			inner.put("cmsArticleWebOld", search);
			param.put("param", inner);

			Map<String, Object> params = new HashMap<>(2);
			params.put("cb", "cb");
			params.put("param", URLEncodeUtil.encode(param.toJSONString()));
			JSONObject json = parseJsonp(getRaw(EF_NEWS_URL, params));
			if (json.getIntValue("code") != 0) {
				throw new IllegalStateException("东方财富新闻接口调用失败: " + json.getString("msg"));
			}
			JSONObject result = json.getJSONObject("result");
			JSONArray items = result == null ? null : result.getJSONArray("cmsArticleWebOld");
			if (items == null || items.isEmpty()) {
				break;
			}
			for (int i = 0; i < items.size(); i++) {
				JSONObject it = items.getJSONObject(i);
				LocalDate pub = parseDateStr(it.getString("date"));
				if (pub == null || pub.isBefore(startDate)) {
					break outer;
				}
				if (pub.isAfter(endDate)) {
					continue;
				}
				StockMotAnnNewsEntity entity = new StockMotAnnNewsEntity();
				entity.setTsCode(tsCode);
				entity.setPubDate(pub.format(DATE_FMT));
				entity.setPubDatetime(it.getString("date"));
				entity.setNewsType("media");
				entity.setSrc(StrUtil.blankToDefault(it.getString("mediaName"), "东方财富"));
				entity.setTitle(clean(it.getString("title")));
				// 东财搜索返回 content 全文，按"简短摘要"要求截断存储
				entity.setSummary(cleanTruncate(it.getString("content")));
				entity.setUrl(it.getString("url"));
				list.add(entity);
			}
			if (items.size() < EF_NEWS_PAGE_SIZE) {
				break;
			}
		}
		return list;
	}

	/**
	 * 巨潮资讯：交易所公告（降级数据源）
	 */
	private List<StockMotAnnNewsEntity> fetchCninfo(String tsCode, LocalDate startDate, LocalDate endDate) {
		String code = tsCode.split("\\.")[0];
		String suffix = tsCode.endsWith(".SH") ? "sh" : "sz";
		String column = "sse".equals(suffix) ? "sse" : "szse";
		// orgId 动态解析：部分股票（如 A+H 股比亚迪）orgId 非 gs+交易所+代码 规则（gshk0001211），必须查询
		String orgId = resolveOrgId(code);
		String stockParam = code + "," + orgId;
		String seDate = startDate.format(CNINFO_DATE_FMT) + "~" + endDate.format(CNINFO_DATE_FMT);

		List<StockMotAnnNewsEntity> list = new ArrayList<>();
		for (int page = 1; page <= CNINFO_MAX_PAGES; page++) {
			Map<String, Object> form = new HashMap<>(16);
			form.put("pageNum", page);
			form.put("pageSize", CNINFO_PAGE_SIZE);
			form.put("column", column);
			form.put("tabName", "fulltext");
			form.put("plate", "");
			form.put("stock", stockParam);
			form.put("searchkey", "");
			form.put("secid", "");
			form.put("category", "");
			form.put("trade", "");
			form.put("seDate", seDate);
			form.put("sortName", "");
			form.put("sortType", "");
			form.put("isHLtitle", "false");

			String body = HttpRequest.post(CNINFO_URL)
					.header("Content-Type", "application/x-www-form-urlencoded")
					.header("User-Agent", CNINFO_UA)
					.form(form)
					.timeout(15000)
					.execute()
					.body();
			JSONObject json = JSON.parseObject(body);
			if (json == null) {
				throw new IllegalStateException("巨潮资讯接口响应为空");
			}
			JSONArray items = json.getJSONArray("announcements");
			boolean hasMore = json.getBooleanValue("hasMore");
			if (items == null || items.isEmpty()) {
				break;
			}
			for (int i = 0; i < items.size(); i++) {
				JSONObject it = items.getJSONObject(i);
				LocalDate pub = toLocalDate(it.getLongValue("announcementTime"));
				if (pub.isBefore(startDate) || pub.isAfter(endDate)) {
					continue;
				}
				StockMotAnnNewsEntity entity = new StockMotAnnNewsEntity();
				entity.setTsCode(tsCode);
				entity.setPubDate(pub.format(DATE_FMT));
				entity.setPubDatetime(toDatetime(it.getLongValue("announcementTime")));
				entity.setNewsType("ann");
				entity.setSrc("巨潮资讯");
				entity.setTitle(clean(it.getString("announcementTitle")));
				entity.setUrl(StrUtil.blankToDefault(it.getString("adjunctUrl"), "")
						.startsWith("http") ? it.getString("adjunctUrl")
						: CNINFO_FILE_PREFIX + it.getString("adjunctUrl"));
				list.add(entity);
			}
			if (!hasMore) {
				break;
			}
		}
		return list;
	}

	/**
	 * 解析巨潮代码对应的 orgId（topSearch 接口），失败时按规则推导兜底
	 * <p>
	 * 巨潮 orgId 不总是 gs+交易所+代码（如比亚迪为 gshk0001211），必须实时解析
	 */
	private String resolveOrgId(String code) {
		try {
			String body = HttpRequest.post(CNINFO_SEARCH_URL)
					.header("User-Agent", CNINFO_UA)
					.form(Map.of("keyWord", code, "maxNum", 10))
					.timeout(15000)
					.execute()
					.body();
			JSONArray arr = JSON.parseArray(body);
			if (arr != null) {
				for (int i = 0; i < arr.size(); i++) {
					JSONObject o = arr.getJSONObject(i);
					if (code.equals(o.getString("code")) && StrUtil.isNotBlank(o.getString("orgId"))) {
						return o.getString("orgId");
					}
				}
			}
			log.warn("巨潮未匹配到 {} 的 orgId，使用规则推导", code);
		}
		catch (Exception e) {
			log.warn("巨潮 orgId 解析失败，使用规则推导: {}", e.getMessage());
		}
		// 兜底：按代码前缀推导
		return "gs" + (code.startsWith("6") ? "sh" : "sz") + code;
	}

	/**
	 * 按唯一键 (ts_code, pub_date, news_type, url) 批量插入/更新
	 */
	private int upsertByUniqueKey(List<StockMotAnnNewsEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		StockMotAnnNewsEntity first = rows.get(0);
		List<StockMotAnnNewsEntity> existList = list(Wrappers.<StockMotAnnNewsEntity>lambdaQuery()
				.select(StockMotAnnNewsEntity::getId, StockMotAnnNewsEntity::getPubDate,
						StockMotAnnNewsEntity::getNewsType, StockMotAnnNewsEntity::getUrl)
				.eq(StockMotAnnNewsEntity::getTsCode, first.getTsCode()));
		Map<String, StockMotAnnNewsEntity> existMap = existList.stream()
				.collect(Collectors.toMap(this::uniqueKey, e -> e));

		List<StockMotAnnNewsEntity> toInsert = new ArrayList<>();
		List<StockMotAnnNewsEntity> toUpdate = new ArrayList<>();
		for (StockMotAnnNewsEntity row : rows) {
			StockMotAnnNewsEntity exist = existMap.get(uniqueKey(row));
			if (exist == null) {
				toInsert.add(row);
			}
			else {
				row.setId(exist.getId());
				toUpdate.add(row);
			}
		}
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert, BATCH_SIZE);
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, BATCH_SIZE);
		}
		return rows.size();
	}

	/**
	 * 唯一键：pub_date + news_type + url（对应唯一索引 uk_mot_ann_news）
	 */
	private String uniqueKey(StockMotAnnNewsEntity e) {
		return e.getPubDate() + "|" + e.getNewsType() + "|" + StrUtil.nullToEmpty(e.getUrl());
	}

	/**
	 * GET 请求并解析 JSON（校验 success）
	 */
	private JSONObject getJson(String url, Map<String, Object> params) {
		JSONObject json = JSON.parseObject(getRaw(url, params));
		if (json == null || !json.getBooleanValue("success")) {
			String msg = json != null ? json.getString("message") : "空响应";
			throw new IllegalStateException("东方财富接口调用失败: " + msg);
		}
		return json;
	}

	/**
	 * GET 请求返回原始响应体
	 */
	private String getRaw(String url, Map<String, Object> params) {
		return HttpRequest.get(url)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.timeout(15000)
				.execute()
				.body();
	}

	/**
	 * 解析 jsonp 响应：cb({...}) → JSONObject
	 */
	private JSONObject parseJsonp(String body) {
		if (StrUtil.isBlank(body)) {
			throw new IllegalStateException("东方财富新闻接口响应为空");
		}
		int start = body.indexOf('(');
		int end = body.lastIndexOf(')');
		if (start < 0 || end <= start) {
			throw new IllegalStateException("jsonp 响应解析失败");
		}
		return JSON.parseObject(body.substring(start + 1, end));
	}

	/**
	 * 毫秒时间戳 → 日期（北京时间）
	 */
	private LocalDate toLocalDate(long millis) {
		return Instant.ofEpochMilli(millis).atZone(CN_ZONE).toLocalDate();
	}

	/**
	 * 毫秒时间戳 → 精确时间字符串（北京时间）
	 */
	private String toDatetime(long millis) {
		return Instant.ofEpochMilli(millis).atZone(CN_ZONE).format(DATETIME_FMT);
	}

	/**
	 * 解析日期字符串（兼容 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd"），前 10 位取日期
	 */
	private LocalDate parseDateStr(String value) {
		if (StrUtil.isBlank(value)) {
			return null;
		}
		String v = value.trim();
		try {
			if (v.length() >= 10) {
				return LocalDate.parse(v.substring(0, 10));
			}
			return LocalDate.parse(v);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * 清洗标题/摘要：去 HTML 标签（东财搜索结果带 &lt;em&gt; 高亮）
	 */
	private String clean(String value) {
		return StrUtil.isBlank(value) ? null : HtmlUtil.cleanHtmlTag(value).trim();
	}

	/**
	 * 清洗并截断为简短摘要（默认 500 字，不存全文）
	 */
	private String cleanTruncate(String value) {
		String cleaned = clean(value);
		return cleaned == null ? null : StrUtil.maxLength(cleaned, 500);
	}

}
