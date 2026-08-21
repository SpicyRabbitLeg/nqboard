package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.vo.StockDailyKlineVO;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.service.StockDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Tushare日线行情 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDailyServiceImpl extends ServiceImpl<StockDailyMapper, StockDailyEntity>
		implements StockDailyService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";
	private static final String API_NAME_DAILY = "daily";

	/**
	 * 单请求最大重试次数（网络抖动/限频退避重试）
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 重试基础退避间隔（毫秒），第 n 次重试等待 base * n
	 */
	private static final long RETRY_BASE_DELAY_MS = 1000L;

	/**
	 * 全量同步起始日期：2026-01-01
	 */
	private static final LocalDate FULL_SYNC_START = LocalDate.of(2026, 1, 1);

	/**
	 * 增量同步自愈回看天数（日历日）：增量起点 = 表内最新交易日 - N 天，
	 * 单日任务失败后下次运行自动回补，无需人工重跑全量
	 */
	private static final int INCREMENTAL_SELF_HEAL_DAYS = 7;

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 市场过滤值：全部
	 */
	private static final String MARKET_ALL = "全部";

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 相邻股票请求间隔（毫秒）
	 * <p>
	 * daily 接口限频 500 次/分钟（120ms/次），直接写死，留安全余量
	 */
	private static final long REQUEST_INTERVAL_MS = 200L;

	private final StockDailyMapper stockDailyMapper;

	private final StockBasicMapper stockBasicMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 日线同步市场过滤（yml 配置 tushare.daily.market）
	 */
	@Value("${tushare.daily.market:全部}")
	private String syncMarket;

	/**
	 * 是否全量同步（yml 配置 tushare.daily.full）
	 */
	@Value("${tushare.daily.full:false}")
	private boolean syncFull;

	/**
	 * 无参重载，便于 Quartz 定时任务在未配置参数时直接调用，取 yml 配置
	 * @return 同步成功的条数
	 */
	@Override
	public int syncFromTushare() {
		return syncFromTushare(null, null);
	}

	/**
	 * 查询K线数据：按股票代码返回最新 limit 根日线，按交易日期正序
	 */
	@Override
	public List<StockDailyKlineVO> kline(String tsCode, Integer limit) {
		int size = limit != null && limit > 0 ? limit : 2000;
		Page<StockDailyEntity> page = page(new Page<>(1, size),
				Wrappers.<StockDailyEntity>lambdaQuery()
						.eq(StockDailyEntity::getTsCode, tsCode)
						.orderByDesc(StockDailyEntity::getTradeDate));
		List<StockDailyEntity> records = page.getRecords();
		// 倒序翻成正序，供前端K线图按时间轴从左到右渲染
		Collections.reverse(records);
		return BeanUtil.copyToList(records, StockDailyKlineVO.class);
	}

	/**
	 * 从 tushare 同步股票日线行情
	 * <p>
	 * 流程：读取 stock_basic 全量股票代码（按市场过滤）→ 逐股票调用 tushare daily 接口
	 * → 按唯一键 (ts_code, trade_date) 批量插入/更新
	 */
	@Override
	public int syncFromTushare(String market, Boolean full) {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		// 入参优先，未传则取 yml 配置
		String syncMarket = StrUtil.blankToDefault(market, this.syncMarket);
		boolean syncFull = full != null ? full : this.syncFull;

		LocalDate endDate = LocalDate.now();
		// 增量起点自愈：表内最新交易日前推 N 天（单日漏跑自动回补）；表为空则退化为全量起点
		LocalDate startDate;
		if (syncFull) {
			startDate = FULL_SYNC_START;
		}
		else {
			LocalDate latest = latestTradeDate();
			startDate = latest != null ? latest.minusDays(INCREMENTAL_SELF_HEAL_DAYS) : FULL_SYNC_START;
		}
		String start = startDate.format(BASIC_DATE);
		String end = endDate.format(BASIC_DATE);
		log.info("开始从 tushare 同步日线行情, market={}, full={}, 区间: {} ~ {}", syncMarket, syncFull, start, end);

		// 1. 从 stock_basic 获取待同步的股票代码（按市场过滤）
		List<StockBasicEntity> basics = stockBasicMapper.selectList(Wrappers.<StockBasicEntity>lambdaQuery()
				.eq(!isAllMarket(syncMarket), StockBasicEntity::getMarket, syncMarket));
		if (CollUtil.isEmpty(basics)) {
			log.warn("stock_basic 无待同步股票，请先同步股票基础信息或调整市场过滤 market={}", syncMarket);
			return 0;
		}
		List<String> tsCodes = basics.stream().map(StockBasicEntity::getTsCode)
				.filter(StrUtil::isNotBlank).distinct().toList();
		log.info("待同步股票数: {}，市场过滤: {}", tsCodes.size(), syncMarket);

		// 2. 逐股票拉取日线并落库
		int total = 0;
		int failCount = 0;
		for (int i = 0; i < tsCodes.size(); i++) {
			String tsCode = tsCodes.get(i);
			try {
				List<StockDailyEntity> rows = fetchDaily(tsCode, start, end);
				int affected = upsertByUniqueKey(tsCode, rows);
				total += affected;
				if ((i + 1) % 200 == 0 || i == tsCodes.size() - 1) {
					log.info("日线同步进度: {}/{}, 累计影响 {} 行, 失败 {} 只", i + 1, tsCodes.size(), total, failCount);
				}
			}
			catch (Exception e) {
				failCount++;
				log.error("同步 {} 日线失败: {}", tsCode, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("日线同步被中断");
					break;
				}
			}
		}
		log.info("从 tushare 同步日线行情完成, market={}, full={}, 股票数={}, 累计影响 {} 行, 失败 {} 只",
				syncMarket, syncFull, tsCodes.size(), total, failCount);
		// 复权因子回补（按交易日批量，独立失败不影响日线同步结果）
		try {
			int adjCount = syncAdjFactorFromTushare();
			log.info("复权因子回补完成, 影响 {} 行", adjCount);
		}
		catch (Exception e) {
			log.error("复权因子回补失败（不影响日线同步结果，指标计算将按因子缺失=1 降级）: {}", e.getMessage());
		}
		return total;
	}

	/**
	 * 表内最新交易日（自愈起点基准），空表返回 null
	 */
	private LocalDate latestTradeDate() {
		List<Object> dates = stockDailyMapper.selectObjs(Wrappers.<StockDailyEntity>query()
				.select("DISTINCT trade_date")
				.last("order by trade_date desc limit 1"));
		if (CollUtil.isEmpty(dates) || dates.get(0) == null) {
			return null;
		}
		String d = String.valueOf(dates.get(0)).replace("-", "");
		return d.length() == 8 ? LocalDate.parse(d, BASIC_DATE) : null;
	}

	/**
	 * 从 tushare 同步复权因子（adj_factor，按交易日批量：一次调用返回全市场当日因子）
	 * <p>
	 * 只回补 adj_factor 为 NULL 的交易日（每日增量仅 1 次调用；历史初始化按交易日逐日回补）。
	 * 指标计算用前复权口径，消除除权除息日跳空造成的假突破/假超跌。
	 * </p>
	 * @return 影响行数
	 */
	@Override
	public int syncAdjFactorFromTushare() {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		// 待回补交易日：该日存在日线且存在 adj_factor 为空的记录
		List<Object> dates = stockDailyMapper.selectObjs(Wrappers.<StockDailyEntity>query()
				.select("DISTINCT trade_date")
				.isNull("adj_factor")
				.last("order by trade_date asc"));
		if (CollUtil.isEmpty(dates)) {
			return 0;
		}
		log.info("开始回补复权因子, 待回补交易日数: {}", dates.size());
		int total = 0;
		int failCount = 0;
		for (int i = 0; i < dates.size(); i++) {
			String tradeDate = String.valueOf(dates.get(i)).replace("-", "");
			try {
				total += upsertAdjFactor(tradeDate, fetchAdjFactor(tradeDate));
			}
			catch (Exception e) {
				failCount++;
				log.error("回补 {} 复权因子失败: {}", tradeDate, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("复权因子回补被中断");
					break;
				}
			}
		}
		log.info("复权因子回补结束, 影响行数={}, 失败交易日数={}", total, failCount);
		return total;
	}

	/**
	 * 调用 tushare adj_factor 接口拉取单个交易日的全市场复权因子
	 */
	private Map<String, Float> fetchAdjFactor(String tradeDate) {
		JSONObject data = postTushare("adj_factor", Map.of("trade_date", tradeDate));
		if (data == null) {
			return Collections.emptyMap();
		}
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");
		if (items == null || items.isEmpty()) {
			return Collections.emptyMap();
		}
		int codeIdx = fields.indexOf("ts_code");
		int factorIdx = fields.indexOf("adj_factor");
		if (codeIdx < 0 || factorIdx < 0) {
			return Collections.emptyMap();
		}
		Map<String, Float> factorByTs = new HashMap<>(items.size());
		for (int i = 0; i < items.size(); i++) {
			JSONArray item = items.getJSONArray(i);
			String tsCode = strVal(item, codeIdx);
			Float factor = floatVal(item, factorIdx);
			if (StrUtil.isNotBlank(tsCode) && factor != null) {
				factorByTs.put(tsCode, factor);
			}
		}
		return factorByTs;
	}

	/**
	 * 按交易日批量更新复权因子（只更新当日 adj_factor 为空的行）
	 */
	private int upsertAdjFactor(String tradeDate, Map<String, Float> factorByTs) {
		if (factorByTs.isEmpty()) {
			return 0;
		}
		List<StockDailyEntity> rows = list(Wrappers.<StockDailyEntity>lambdaQuery()
				.select(StockDailyEntity::getId, StockDailyEntity::getTsCode)
				.eq(StockDailyEntity::getTradeDate, tradeDate)
				.isNull(StockDailyEntity::getAdjFactor));
		List<StockDailyEntity> toUpdate = new ArrayList<>();
		for (StockDailyEntity row : rows) {
			Float factor = factorByTs.get(row.getTsCode());
			if (factor != null) {
				row.setAdjFactor(factor);
				toUpdate.add(row);
			}
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, BATCH_SIZE);
		}
		return toUpdate.size();
	}

	/**
	 * tushare 通用 POST 请求（15s 超时 + 3 次退避重试），返回 data 节点
	 */
	private JSONObject postTushare(String apiName, Map<String, Object> tsParams) {
		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				return doPostTushare(apiName, tsParams);
			}
			catch (Exception e) {
				last = e;
				log.warn("tushare {} 接口调用失败(第 {}/{} 次): {}", apiName, attempt, MAX_RETRY, e.getMessage());
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
		throw new IllegalStateException("tushare " + apiName + " 接口请求失败: "
				+ (last != null ? last.getMessage() : "unknown"), last);
	}

	/**
	 * tushare 单次 POST 请求
	 */
	private JSONObject doPostTushare(String apiName, Map<String, Object> tsParams) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", apiName);
		params.put("token", token);
		params.put("params", tsParams);

		String respBody = HttpRequest.post(TUSHARE_URL)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(params))
				.timeout(15000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getIntValue("code") != 0) {
			String msg = body != null ? body.getString("msg") : "空响应";
			throw new IllegalStateException("tushare " + apiName + " 接口调用失败: " + msg);
		}
		return body.getJSONObject("data");
	}

	/**
	 * 调用 tushare daily 接口拉取单只股票日线（带超时与重试）
	 */
	private List<StockDailyEntity> fetchDaily(String tsCode, String start, String end) {
		JSONObject data = postTushare(API_NAME_DAILY, Map.of("ts_code", tsCode, "start_date", start, "end_date", end));
		if (data == null) {
			return Collections.emptyList();
		}
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");
		if (items == null || items.isEmpty()) {
			return Collections.emptyList();
		}
		List<StockDailyEntity> list = new ArrayList<>(items.size());
		for (int i = 0; i < items.size(); i++) {
			StockDailyEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity != null && StrUtil.isNotBlank(entity.getTsCode()) && StrUtil.isNotBlank(entity.getTradeDate())) {
				list.add(entity);
			}
		}
		return list;
	}

	/**
	 * 按唯一键 (ts_code, trade_date) 批量插入/更新
	 * <p>
	 * 先查该股票已有交易日期，区间内已存在的记录走 update（保留原 id），其余走批量插入，
	 * 避免全量重跑时对唯一索引 uk_ts_trade 的冲突
	 */
	private int upsertByUniqueKey(String tsCode, List<StockDailyEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		// Tushare 返回的数据中可能存在重复记录，先按 trade_date 去重，避免唯一键冲突
		Map<String, StockDailyEntity> uniqueRows = new HashMap<>();
		for (StockDailyEntity row : rows) {
			uniqueRows.putIfAbsent(row.getTradeDate(), row);
		}
		rows = new ArrayList<>(uniqueRows.values());
		List<StockDailyEntity> existList = list(Wrappers.<StockDailyEntity>lambdaQuery()
				.select(StockDailyEntity::getId, StockDailyEntity::getTradeDate)
				.eq(StockDailyEntity::getTsCode, tsCode));
		Map<String, StockDailyEntity> existMap = existList.stream()
				.collect(Collectors.toMap(StockDailyEntity::getTradeDate, e -> e));

		List<StockDailyEntity> toInsert = new ArrayList<>();
		List<StockDailyEntity> toUpdate = new ArrayList<>();
		for (StockDailyEntity row : rows) {
			StockDailyEntity exist = existMap.get(row.getTradeDate());
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
	 * 按 fields 顺序把 items 行映射为实体
	 */
	private StockDailyEntity mapRow(List<String> fields, JSONArray item) {
		StockDailyEntity entity = new StockDailyEntity();
		for (int i = 0; i < fields.size(); i++) {
			switch (fields.get(i)) {
				case "ts_code" -> entity.setTsCode(strVal(item, i));
				case "trade_date" -> entity.setTradeDate(strVal(item, i));
				case "open" -> entity.setOpen(floatVal(item, i));
				case "high" -> entity.setHigh(floatVal(item, i));
				case "low" -> entity.setLow(floatVal(item, i));
				case "close" -> entity.setClose(floatVal(item, i));
				case "pre_close" -> entity.setPreClose(floatVal(item, i));
				case "change" -> entity.setChange(floatVal(item, i));
				case "pct_chg" -> entity.setPctChg(floatVal(item, i));
				case "vol" -> entity.setVol(floatVal(item, i));
				case "amount" -> entity.setAmount(floatVal(item, i));
				case "ah_vol" -> entity.setAhVol(floatVal(item, i));
				case "ah_amount" -> entity.setAhAmount(floatVal(item, i));
				case "adj_factor" -> entity.setAdjFactor(floatVal(item, i));
				default -> log.debug("忽略未知字段: {}", fields.get(i));
			}
		}
		return entity;
	}

	/**
	 * 字符串取值，JSON null / "null" 转为 null
	 */
	private String strVal(JSONArray item, int i) {
		String s = item.getString(i);
		return StrUtil.isBlank(s) || "null".equals(s) ? null : s;
	}

	/**
	 * 浮点取值，JSON null / "null" / 空串 转为 null
	 */
	private Float floatVal(JSONArray item, int i) {
		String s = item.getString(i);
		if (StrUtil.isBlank(s) || "null".equals(s)) {
			return null;
		}
		return Float.parseFloat(s.trim());
	}

	/**
	 * 是否同步全部市场（配置为空或"全部"时不过滤）
	 */
	private boolean isAllMarket(String market) {
		return StrUtil.isBlank(market) || MARKET_ALL.equals(market) || "all".equalsIgnoreCase(market);
	}

}
