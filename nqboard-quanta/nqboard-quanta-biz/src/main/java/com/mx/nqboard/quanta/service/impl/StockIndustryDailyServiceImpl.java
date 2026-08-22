package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.dto.SyncResult;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.config.QuantSyncLog;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.service.StockIndustryDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 行业板块日线K线 服务实现类
 * </p>
 * <p>
 * 数据来源：东财 clist 板块列表（fs=m:90+t:2 行业板块）+ push2his 板块K线（secid=90.BKxxxx），
 * 东财不可用时自动降级到新浪财经行业板块。
 * 板块数量约 86 个，请求间隔可由 yml 配置（industry-daily.request-interval-ms），
 * push2his CDN 对高频请求会临时封锁，默认间隔 3 秒（全量约 4~5 分钟）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndustryDailyServiceImpl extends ServiceImpl<StockIndustryDailyMapper, StockIndustryDailyEntity>
		implements StockIndustryDailyService {

	/**
	 * 东财 clist 板块列表接口
	 */
	private static final String CLIST_URL = "https://push2.eastmoney.com/api/qt/clist/get";

	/**
	 * 东财 push2his K线接口
	 */
	private static final String KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";

	/**
	 * 新浪财经行业板块列表接口（东财不可用时的降级数据源）
	 */
	private static final String SINA_BOARD_LIST_URL = "https://vip.stock.finance.sina.com.cn/q/view/newSinaHy.php";

	/**
	 * 新浪财经行业板块K线接口
	 */
	private static final String SINA_KLINE_URL = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

	/**
	 * 行业板块列表过滤条件
	 */
	private static final String FS_INDUSTRY_BOARD = "m:90+t:2+f:!50";

	/**
	 * 东财行业名 -> 新浪行业名映射（用于降级时按名称匹配新浪板块）
	 * 注意：新浪实际行业名可能随页面调整，若匹配不到会退回使用东财原名匹配。
	 */
	private static final Map<String, String> EASTMONEY_TO_SINA_BOARD_NAME = new HashMap<>();

	static {
		EASTMONEY_TO_SINA_BOARD_NAME.put("银行", "银行");
		EASTMONEY_TO_SINA_BOARD_NAME.put("保险", "保险");
		EASTMONEY_TO_SINA_BOARD_NAME.put("证券", "证券");
		EASTMONEY_TO_SINA_BOARD_NAME.put("酿酒行业", "酿酒行业");
		EASTMONEY_TO_SINA_BOARD_NAME.put("食品饮料", "食品行业");
		EASTMONEY_TO_SINA_BOARD_NAME.put("医药制造", "生物制药");
		EASTMONEY_TO_SINA_BOARD_NAME.put("医疗器械", "医疗器械");
		EASTMONEY_TO_SINA_BOARD_NAME.put("电子元件", "电子");
		EASTMONEY_TO_SINA_BOARD_NAME.put("电子信息", "电子信息");
		EASTMONEY_TO_SINA_BOARD_NAME.put("软件服务", "软件");
		EASTMONEY_TO_SINA_BOARD_NAME.put("通讯行业", "通信");
		EASTMONEY_TO_SINA_BOARD_NAME.put("互联网", "互联网");
		EASTMONEY_TO_SINA_BOARD_NAME.put("化工行业", "化工");
		EASTMONEY_TO_SINA_BOARD_NAME.put("有色金属", "有色金属");
		EASTMONEY_TO_SINA_BOARD_NAME.put("贵金属", "贵金属");
		EASTMONEY_TO_SINA_BOARD_NAME.put("煤炭行业", "煤炭");
		EASTMONEY_TO_SINA_BOARD_NAME.put("石油行业", "石油");
		EASTMONEY_TO_SINA_BOARD_NAME.put("电力行业", "电力");
		EASTMONEY_TO_SINA_BOARD_NAME.put("钢铁行业", "钢铁");
		EASTMONEY_TO_SINA_BOARD_NAME.put("水泥建材", "水泥");
		EASTMONEY_TO_SINA_BOARD_NAME.put("玻璃陶瓷", "玻璃");
		EASTMONEY_TO_SINA_BOARD_NAME.put("工程建设", "建筑");
		EASTMONEY_TO_SINA_BOARD_NAME.put("房地产", "房地产");
		EASTMONEY_TO_SINA_BOARD_NAME.put("汽车行业", "汽车");
		EASTMONEY_TO_SINA_BOARD_NAME.put("家电行业", "家电");
		EASTMONEY_TO_SINA_BOARD_NAME.put("商业百货", "商业百货");
		EASTMONEY_TO_SINA_BOARD_NAME.put("旅游酒店", "酒店旅游");
		EASTMONEY_TO_SINA_BOARD_NAME.put("民航机场", "机场");
		EASTMONEY_TO_SINA_BOARD_NAME.put("港口水运", "交通运输");
		EASTMONEY_TO_SINA_BOARD_NAME.put("高速公路", "公路");
		EASTMONEY_TO_SINA_BOARD_NAME.put("交运物流", "物流");
		EASTMONEY_TO_SINA_BOARD_NAME.put("农牧饲渔", "农业");
		EASTMONEY_TO_SINA_BOARD_NAME.put("农药兽药", "农药化肥");
		EASTMONEY_TO_SINA_BOARD_NAME.put("化肥行业", "农药化肥");
		EASTMONEY_TO_SINA_BOARD_NAME.put("纺织服装", "纺织");
		EASTMONEY_TO_SINA_BOARD_NAME.put("造纸印刷", "造纸");
		EASTMONEY_TO_SINA_BOARD_NAME.put("包装材料", "包装");
		EASTMONEY_TO_SINA_BOARD_NAME.put("木业家具", "家具");
		EASTMONEY_TO_SINA_BOARD_NAME.put("化纤行业", "化纤");
		EASTMONEY_TO_SINA_BOARD_NAME.put("航天航空", "航天军工");
		EASTMONEY_TO_SINA_BOARD_NAME.put("船舶制造", "船舶");
		EASTMONEY_TO_SINA_BOARD_NAME.put("机械行业", "机械");
		EASTMONEY_TO_SINA_BOARD_NAME.put("仪器仪表", "仪器");
		EASTMONEY_TO_SINA_BOARD_NAME.put("专用设备", "机械");
		EASTMONEY_TO_SINA_BOARD_NAME.put("通用设备", "机械");
		EASTMONEY_TO_SINA_BOARD_NAME.put("安防设备", "安防");
		EASTMONEY_TO_SINA_BOARD_NAME.put("输配电气", "电气");
		EASTMONEY_TO_SINA_BOARD_NAME.put("文化传媒", "传媒");
		EASTMONEY_TO_SINA_BOARD_NAME.put("电信运营", "通信");
		EASTMONEY_TO_SINA_BOARD_NAME.put("综合行业", "综合");
		EASTMONEY_TO_SINA_BOARD_NAME.put("公用事业", "公用事业");
		EASTMONEY_TO_SINA_BOARD_NAME.put("环保工程", "环保");
		EASTMONEY_TO_SINA_BOARD_NAME.put("园林工程", "园林");
		EASTMONEY_TO_SINA_BOARD_NAME.put("材料行业", "材料");
		EASTMONEY_TO_SINA_BOARD_NAME.put("金属制品", "金属制品");
		EASTMONEY_TO_SINA_BOARD_NAME.put("工艺商品", "工艺");
		EASTMONEY_TO_SINA_BOARD_NAME.put("文教休闲", "文教");
		EASTMONEY_TO_SINA_BOARD_NAME.put("珠宝首饰", "珠宝");
		EASTMONEY_TO_SINA_BOARD_NAME.put("非金属品", "非金属");
		EASTMONEY_TO_SINA_BOARD_NAME.put("煤炭采选", "煤炭");
		EASTMONEY_TO_SINA_BOARD_NAME.put("石油矿业", "石油");
		EASTMONEY_TO_SINA_BOARD_NAME.put("矿业", "有色金属");
	}


	/**
	 * 板块市场前缀（东财 secid 规则：90=板块）
	 */
	private static final String BOARD_SECID_PREFIX = "90.";

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 单板块请求最大重试次数
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 重试基础退避间隔（毫秒），第 n 次重试等待 base * n
	 */
	private static final long RETRY_BASE_DELAY_MS = 3000L;

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockIndustryDailyMapper stockIndustryDailyMapper;
	private final StockBasicMapper stockBasicMapper;

	private final StockDailyMapper stockDailyMapper;

	/**
	 * 相邻板块请求间隔（毫秒），yml 配置 industry-daily.request-interval-ms
	 */
	@Value("${industry-daily.request-interval-ms:3000}")
	private long requestIntervalMs;

	/**
	 * 是否全量同步（yml 配置 industry-daily.full）
	 */
	@Value("${industry-daily.full:false}")
	private boolean syncFull;

	@Override
	@QuantSyncLog(type = "industry_daily", name = "行业板块日线同步")
	public SyncResult syncFromEastMoney() {
		return syncFromEastMoney(null);
	}

	@Override
	@QuantSyncLog(type = "industry_daily", name = "行业板块日线同步")
	public SyncResult syncFromEastMoney(Boolean full) {
		boolean syncFull = full != null ? full : this.syncFull;
		// 增量起点自愈：表内最新交易日前推 7 天（单日漏跑自动回补）；表为空则退化为全量起点
		String beg;
		if (syncFull) {
			beg = LocalDate.of(2026, 1, 1).format(BASIC_DATE);
		}
		else {
			LocalDate latest = latestTradeDate();
			LocalDate start = latest != null ? latest.minusDays(7) : LocalDate.of(2026, 1, 1);
			beg = start.format(BASIC_DATE);
		}

		List<Map<String, String>> boards = fetchBoardList();
		if (CollUtil.isEmpty(boards)) {
			throw new IllegalStateException("东方财富 板块列表接口无数据返回");
		}
		log.info("开始同步行业板块日线, 板块数={}, full={}, beg={}", boards.size(), syncFull, beg);

		int total = 0;
		int failCount = 0;
		for (int i = 0; i < boards.size(); i++) {
			String boardCode = boards.get(i).get("code");
			String boardName = boards.get(i).get("name");
			try {
				List<StockIndustryDailyEntity> rows = fetchKlineWithFallback(boardCode, boardName, beg);
				total += upsertByUniqueKey(boardCode, rows);
			}
			catch (Exception e) {
				failCount++;
				log.error("同步板块 {}({}) 日线失败: {}", boardCode, boardName, e.getMessage());
			}
			if ((i + 1) % 20 == 0) {
				log.info("板块日线同步进度: {}/{}, 累计影响 {} 行, 失败 {} 个", i + 1, boards.size(), total, failCount);
			}
			if (requestIntervalMs > 0) {
				try {
					Thread.sleep(requestIntervalMs);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("板块日线同步被中断");
					break;
				}
			}
		}
		if (total == 0) {
			log.warn("外部行业板块K线无数据，使用本地 stock_daily 内部计算兜底");
			List<StockIndustryDailyEntity> localRows = buildLocalIndustryDaily(beg);
			for (StockIndustryDailyEntity row : localRows) {
				total += upsertByUniqueKey(row.getBoardCode(), List.of(row));
			}
			log.info("本地行业板块兜底完成, 生成 {} 条", localRows.size());
		}

		log.info("行业板块日线同步完成, 板块数={}, 累计影响 {} 行, 失败 {} 个", boards.size(), total, failCount);
		return SyncResult.builder()
				.affected(total)
				.successCount(total)
				.failCount(failCount)
				.totalCount(boards.size())
				.syncRange("beg=" + beg)
				.message("同步 " + boards.size() + " 个板块, 影响 " + total + " 行, 失败 " + failCount + " 个")
				.build();
	}

	/**
	 * 拉取行业板块列表 [{code: BK0475, name: 银行}]
	 */
	private List<Map<String, String>> fetchBoardList() {
		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				List<Map<String, String>> boards = doFetchBoardList();
			if (CollUtil.isEmpty(boards)) {
				throw new IllegalStateException("东方财富行业板块列表为空");
			}
			return boards;
			}
			catch (Exception e) {
				last = e;
				log.warn("行业板块列表请求失败(第 {}/{} 次): {}", attempt, MAX_RETRY, e.getMessage());
				if (attempt < MAX_RETRY) {
					try {
						Thread.sleep(RETRY_BASE_DELAY_MS * attempt);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		log.warn("东方财富行业板块列表重试仍失败，切换到新浪财经: {}", last.getMessage());
		return fetchSinaBoardList();
	}


	private List<Map<String, String>> doFetchBoardList() {
		Map<String, Object> params = Map.of(
				"pn", 1,
				"pz", 200,
				"po", 1,
				"np", 1,
				"fltt", 2,
				"invt", 2,
				"fid", "f3",
				"fs", FS_INDUSTRY_BOARD,
				"fields", "f12,f14");

		String respBody = HttpRequest.get(CLIST_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://quote.eastmoney.com/")
				.header("Accept", "*/*")
				.header("Accept-Language", "zh-CN,zh;q=0.9")
				.header("Connection", "close")
				.timeout(15000)
				.execute()
				.body();
		if (StrUtil.isBlank(respBody)) {
			throw new IllegalStateException("东方财富 行业板块列表接口响应为空");
		}
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getJSONObject("data") == null) {
			return new ArrayList<>();
		}
		JSONArray diff = body.getJSONObject("data").getJSONArray("diff");
		List<Map<String, String>> boards = new ArrayList<>();
		if (diff != null) {
			for (int i = 0; i < diff.size(); i++) {
				JSONObject item = diff.getJSONObject(i);
				String code = item.getString("f12");
				String name = item.getString("f14");
				if (StrUtil.isNotBlank(code) && code.startsWith("BK")) {
					boards.add(Map.of("code", code, "name", StrUtil.nullToEmpty(name)));
				}
			}
		}
		return boards;
	}

	/**
	 * 新浪财经行业板块列表（降级数据源）
	 */
	private List<Map<String, String>> fetchSinaBoardList() {
		String respBody = HttpRequest.get(SINA_BOARD_LIST_URL)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://finance.sina.com.cn")
				.timeout(15000)
				.execute()
				.body();
		if (StrUtil.isBlank(respBody)) {
			throw new IllegalStateException("新浪财经行业板块列表接口响应为空");
		}
		int start = respBody.indexOf('{');
		int end = respBody.lastIndexOf('}');
		if (start < 0 || end <= start) {
			throw new IllegalStateException("新浪财经行业板块列表响应格式异常: " + StrUtil.maxLength(respBody, 200));
		}
		JSONObject obj = JSON.parseObject(respBody.substring(start, end + 1));
		List<Map<String, String>> boards = new ArrayList<>();
		if (obj != null) {
			for (String code : obj.keySet()) {
				String name = obj.getString(code);
				if (StrUtil.isNotBlank(code) && StrUtil.isNotBlank(name)) {
					boards.add(Map.of("code", code, "name", name));
				}
			}
		}
		if (boards.isEmpty()) {
			throw new IllegalStateException("新浪财经行业板块列表为空");
		}
		log.info("新浪财经行业板块列表加载成功, 板块数={}", boards.size());
		return boards;
	}


	/**
	 * 调用东财 kline 接口拉取单个板块日线（失败自动退避重试）
	 * <p>
	 * klines 每行 CSV：日期,开盘,收盘,最高,最低,成交量,成交额（fields2=f51..f57）
	 */
	private List<StockIndustryDailyEntity> fetchKline(String boardCode, String boardName, String beg) {
		if (boardCode.startsWith("new_")) {
			return fetchSinaKline(boardCode, boardName, beg);
		}

		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				return doFetchKline(boardCode, boardName, beg);
			}
			catch (Exception e) {
				last = e;
				log.warn("板块 {} kline 请求失败(第 {}/{} 次): {}", boardCode, attempt, MAX_RETRY, e.getMessage());
				if (attempt < MAX_RETRY) {
					try {
						Thread.sleep(RETRY_BASE_DELAY_MS * attempt);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		throw new IllegalStateException("东方财富板块 kline 接口请求失败: " + (last != null ? last.getMessage() : "unknown"), last);
	}

	/**
	 * 新浪财经行业板块K线（降级数据源）
	 */
	private List<StockIndustryDailyEntity> fetchSinaKline(String boardCode, String boardName, String beg) {
		String shortSymbol = sinaKlineSymbol(boardCode);
		List<StockIndustryDailyEntity> rows = requestSinaKline(shortSymbol, boardCode, boardName, beg);
		if (rows.isEmpty() && !shortSymbol.equals(boardCode)) {
			log.warn("新浪行业K线 {} 为空，尝试原始 code {}", shortSymbol, boardCode);
			rows = requestSinaKline(boardCode, boardCode, boardName, beg);
		}
		return rows;
	}


	private List<StockIndustryDailyEntity> requestSinaKline(String symbol, String boardCode, String boardName, String beg) {
		Map<String, Object> params = new HashMap<>(8);
		params.put("symbol", symbol);
		params.put("scale", 240);
		params.put("ma", "no");
		params.put("datalen", 1023);

		String respBody = HttpRequest.get(SINA_KLINE_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://finance.sina.com.cn")
				.timeout(15000)
				.execute()
				.body();
		if (StrUtil.isBlank(respBody)) {
			throw new IllegalStateException("新浪财经行业板块K线接口响应为空");
		}
		JSONArray arr = JSON.parseArray(respBody);
		if (arr == null || arr.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockIndustryDailyEntity> list = new ArrayList<>(arr.size());
		for (int i = 0; i < arr.size(); i++) {
			JSONObject item = arr.getJSONObject(i);
			String day = item.getString("day");
			if (StrUtil.isBlank(day)) {
				continue;
			}
			String dayBasic = day.replace("-", "");
			if (dayBasic.compareTo(beg) < 0) {
				continue;
			}
			StockIndustryDailyEntity entity = new StockIndustryDailyEntity();
			entity.setBoardCode(boardCode);
			entity.setBoardName(boardName);
			entity.setTradeDate(dayBasic);
			entity.setOpen(decimal(item.getString("open")));
			entity.setClose(decimal(item.getString("close")));
			entity.setHigh(decimal(item.getString("high")));
			entity.setLow(decimal(item.getString("low")));
			entity.setVolume(decimal(item.getString("volume")));
			entity.setAmount(null);
			list.add(entity);
		}
		return list;
	}

	/**
	 * 新浪行业K线接口需要的 symbol 一般是去掉 new_ 前缀的代码，如 new_blhy -> blhy
	 */
	private String sinaKlineSymbol(String boardCode) {
		return boardCode.startsWith("new_") ? boardCode.substring(4) : boardCode;
	}


	/**
	 * 拉取板块K线：优先东财，失败时按行业名映射到新浪财经降级
	 */
	private List<StockIndustryDailyEntity> fetchKlineWithFallback(String boardCode, String boardName, String beg) {
		try {
			List<StockIndustryDailyEntity> rows = fetchKline(boardCode, boardName, beg);
			if (rows.isEmpty()) {
				log.warn("东财板块 {} K线为空，尝试新浪财经降级", boardCode);
				if (boardCode.startsWith("new_")) {
					return rows;
				}
				String sinaCode = findSinaBoardCode(boardName);
				return fetchSinaKline(sinaCode, boardName, beg);
			}
			return rows;
		}
		catch (Exception e) {
			if (boardCode.startsWith("new_")) {
				throw e;
			}
			log.warn("东财板块 {} K线失败，尝试新浪财经降级: {}", boardCode, e.getMessage());
			String sinaCode = findSinaBoardCode(boardName);
			return fetchSinaKline(sinaCode, boardName, beg);
		}
	}

	/**
	 * 按行业名查找新浪财经板块代码
	 */
	private String findSinaBoardCode(String boardName) {
		String sinaName = EASTMONEY_TO_SINA_BOARD_NAME.getOrDefault(boardName, boardName);
		return fetchSinaBoardList().stream()
				.filter(b -> {
					String sinaBoardName = b.get("name");
					return sinaName.equals(sinaBoardName) || boardName.equals(sinaBoardName)
							|| (sinaBoardName != null
									&& (sinaBoardName.contains(sinaName) || sinaName.contains(sinaBoardName)));
				})
				.map(b -> b.get("code"))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("未找到新浪行业板块: " + boardName));
	}



	/**
	 * 单次调用东财 kline 接口
	 */
	private List<StockIndustryDailyEntity> doFetchKline(String boardCode, String boardName, String beg) {
		Map<String, Object> params = new HashMap<>(8);
		params.put("secid", BOARD_SECID_PREFIX + boardCode);
		params.put("fields1", "f1,f2,f3,f4,f5,f6");
		params.put("fields2", "f51,f52,f53,f54,f55,f56,f57");
		params.put("klt", 101);
		params.put("fqt", 0);
		params.put("beg", beg);
		params.put("end", "20990101");

		String respBody = HttpRequest.get(KLINE_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://quote.eastmoney.com/")
				.header("Accept", "*/*")
				.header("Accept-Language", "zh-CN,zh;q=0.9")
				.header("Connection", "close")
				.timeout(15000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null) {
			throw new IllegalStateException("东方财富板块 kline 接口响应为空");
		}
		JSONObject data = body.getJSONObject("data");
		if (data == null) {
			throw new IllegalStateException("东方财富板块 kline 接口无数据: " + body);
		}
		JSONArray klines = data.getJSONArray("klines");
		if (klines == null || klines.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockIndustryDailyEntity> list = new ArrayList<>(klines.size());
		for (int i = 0; i < klines.size(); i++) {
			String line = klines.getString(i);
			if (StrUtil.isBlank(line)) {
				continue;
			}
			String[] cols = line.split(",");
			if (cols.length < 7) {
				continue;
			}
			StockIndustryDailyEntity entity = new StockIndustryDailyEntity();
			entity.setBoardCode(boardCode);
			entity.setBoardName(boardName);
			entity.setTradeDate(cols[0].trim().replace("-", ""));
			entity.setOpen(decimal(cols[1]));
			entity.setClose(decimal(cols[2]));
			entity.setHigh(decimal(cols[3]));
			entity.setLow(decimal(cols[4]));
			entity.setVolume(decimal(cols[5]));
			entity.setAmount(decimal(cols[6]));
			list.add(entity);
		}
		return list;
	}

	/**
	 * 本地兜底：用 stock_daily + stock_basic.industry 等权平均计算行业板块日线
	 */
	private List<StockIndustryDailyEntity> buildLocalIndustryDaily(String beg) {
		List<StockBasicEntity> basics = stockBasicMapper.selectList(Wrappers.<StockBasicEntity>lambdaQuery()
				.select(StockBasicEntity::getTsCode, StockBasicEntity::getIndustry)
				.isNotNull(StockBasicEntity::getIndustry)
				.ne(StockBasicEntity::getIndustry, ""));
		if (CollUtil.isEmpty(basics)) {
			log.warn("本地行业兜底跳过：stock_basic 无行业信息");
			return new ArrayList<>();
		}
		Map<String, String> industryByTs = new HashMap<>();
		for (StockBasicEntity basic : basics) {
			if (StrUtil.isNotBlank(basic.getIndustry())) {
				industryByTs.put(basic.getTsCode(), basic.getIndustry());
			}
		}

		List<Object> dateObjs = stockDailyMapper.selectObjs(Wrappers.<StockDailyEntity>query()
				.select("DISTINCT trade_date")
				.ge("trade_date", beg)
				.orderByAsc("trade_date"));
		List<StockIndustryDailyEntity> result = new ArrayList<>();
		for (Object obj : dateObjs) {
			String tradeDate = String.valueOf(obj);
			List<StockDailyEntity> dailyList = stockDailyMapper.selectList(Wrappers.<StockDailyEntity>lambdaQuery()
					.eq(StockDailyEntity::getTradeDate, tradeDate));
			Map<String, StockDailyEntity> dailyByTs = dailyList.stream()
					.filter(d -> d.getClose() != null)
					.collect(Collectors.toMap(StockDailyEntity::getTsCode, d -> d, (a, b) -> a));

			Map<String, double[]> acc = new HashMap<>();
			for (Map.Entry<String, String> entry : industryByTs.entrySet()) {
				StockDailyEntity daily = dailyByTs.get(entry.getKey());
				if (daily == null) {
					continue;
				}
				double[] arr = acc.computeIfAbsent(entry.getValue(), k -> new double[5]);
				arr[0] += daily.getOpen() != null ? daily.getOpen().doubleValue() : 0;
				arr[1] += daily.getClose() != null ? daily.getClose().doubleValue() : 0;
				arr[2] += daily.getHigh() != null ? daily.getHigh().doubleValue() : 0;
				arr[3] += daily.getLow() != null ? daily.getLow().doubleValue() : 0;
				arr[4]++;
			}
			for (Map.Entry<String, double[]> entry : acc.entrySet()) {
				String industry = entry.getKey();
				double[] arr = entry.getValue();
				if (arr[4] == 0) {
					continue;
				}
				StockIndustryDailyEntity entity = new StockIndustryDailyEntity();
				entity.setBoardCode("LOCAL_" + industry);
				entity.setBoardName(industry);
				entity.setTradeDate(tradeDate);
				entity.setOpen(BigDecimal.valueOf(arr[0] / arr[4]));
				entity.setClose(BigDecimal.valueOf(arr[1] / arr[4]));
				entity.setHigh(BigDecimal.valueOf(arr[2] / arr[4]));
				entity.setLow(BigDecimal.valueOf(arr[3] / arr[4]));
				entity.setVolume(null);
				entity.setAmount(null);
				result.add(entity);
			}
		}
		log.info("本地行业板块兜底计算完成, 日期数={}, 生成 {} 条", dateObjs.size(), result.size());
		return result;
	}


	/**
	 * 按唯一键 (board_code, trade_date) 批量插入/更新
	 */
	/**
	 * 表内最新交易日（自愈起点基准），空表返回 null
	 */
	private LocalDate latestTradeDate() {
		List<Object> dates = stockIndustryDailyMapper.selectObjs(Wrappers.<StockIndustryDailyEntity>query()
				.select("DISTINCT trade_date")
				.last("order by trade_date desc limit 1"));
		if (CollUtil.isEmpty(dates) || dates.get(0) == null) {
			return null;
		}
		String d = String.valueOf(dates.get(0)).replace("-", "");
		return d.length() == 8 ? LocalDate.parse(d, BASIC_DATE) : null;
	}

	private int upsertByUniqueKey(String boardCode, List<StockIndustryDailyEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		List<StockIndustryDailyEntity> existList = list(Wrappers.<StockIndustryDailyEntity>lambdaQuery()
				.select(StockIndustryDailyEntity::getId, StockIndustryDailyEntity::getTradeDate)
				.eq(StockIndustryDailyEntity::getBoardCode, boardCode));
		Map<String, StockIndustryDailyEntity> existMap = existList.stream()
				.collect(Collectors.toMap(StockIndustryDailyEntity::getTradeDate, e -> e));

		List<StockIndustryDailyEntity> toInsert = new ArrayList<>();
		List<StockIndustryDailyEntity> toUpdate = new ArrayList<>();
		for (StockIndustryDailyEntity row : rows) {
			StockIndustryDailyEntity exist = existMap.get(row.getTradeDate());
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
	 * 数值解析，空串返回 null
	 */
	private BigDecimal decimal(String value) {
		return StrUtil.isBlank(value) ? null : new BigDecimal(value.trim());
	}

}
