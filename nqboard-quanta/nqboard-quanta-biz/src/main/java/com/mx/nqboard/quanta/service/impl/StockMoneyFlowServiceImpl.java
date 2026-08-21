package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.service.StockMoneyFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 个股主力资金流 服务实现类
 * </p>
 * <p>
 * 数据来源：东财 clist 资金流排名接口（fid=f62 主力净流入排序，分页拉全市场）。
 * 该接口为当日快照口径，盘后调用即为当日收盘后的资金流数据。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMoneyFlowServiceImpl extends ServiceImpl<StockMoneyFlowMapper, StockMoneyFlowEntity>
		implements StockMoneyFlowService {

	/**
	 * 东财 clist 资金流排名接口
	 */
	private static final String CLIST_URL = "https://push2.eastmoney.com/api/qt/clist/get";

	/**
	 * 新浪财经个股资金流接口（东财不可用时的降级数据源）
	 */
	private static final String SINA_MONEY_FLOW_URL = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/MoneyFlow.ssl_qsfx_lscjfb";

	/**
	 * 新浪财经资金流兜底请求间隔（毫秒）
	 */
	private static final long SINA_REQUEST_INTERVAL_MS = 200L;

	/**
	 * 沪深A股市场过滤（深主板/创业板/沪主板/科创板）
	 */
	private static final String FS_A_SHARE = "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23";

	/**
	 * 每页条数
	 */
	private static final int PAGE_SIZE = 500;

	/**
	 * 翻页最大页数保护（防止接口异常导致死循环）
	 */
	private static final int MAX_PAGES = 30;

	/**
	 * 翻页请求间隔（毫秒）
	 */
	private static final long PAGE_INTERVAL_MS = 300L;

	/**
	 * 单页请求最大重试次数
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 重试基础退避间隔（毫秒），第 n 次重试等待 base * n
	 */
	private static final long RETRY_BASE_DELAY_MS = 2000L;

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 大盘基准指数（用于解析最新交易日，避免非交易日写入脏数据）
	 */
	private static final String BENCHMARK_INDEX = "sh000300";

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockIndexDailyMapper stockIndexDailyMapper;
	private final StockBasicMapper stockBasicMapper;

	@Override
	public int syncFromEastMoney() {
		String tradeDate = resolveTradeDate();
		if (tradeDate == null) {
			log.warn("资金流同步跳过：无法解析最新交易日（指数日线无数据且当日为非交易日）");
			return 0;
		}
		log.info("开始从 东方财富 同步个股主力资金流, tradeDate={}", tradeDate);

		List<StockMoneyFlowEntity> rows;
		try {
			rows = fetchAllFlows(tradeDate);
		}
		catch (Exception e) {
			log.warn("东方财富资金流同步失败，切换到新浪财经兜底: {}", e.getMessage());
			rows = fetchSinaAllFlows(tradeDate);
		}
		if (CollUtil.isEmpty(rows)) {
			log.warn("东方财富资金流无数据，尝试新浪财经兜底");
			rows = fetchSinaAllFlows(tradeDate);
		}
		if (CollUtil.isEmpty(rows)) {
			log.warn("资金流接口无数据返回");
			return 0;
		}
		int affected = upsertByUniqueKey(tradeDate, rows);
		log.info("资金流同步完成, tradeDate={}, 股票数={}, 影响 {} 行", tradeDate, rows.size(), affected);
		return affected;
	}

	/**
	 * 分页拉取全市场资金流快照
	 */
	private List<StockMoneyFlowEntity> fetchAllFlows(String tradeDate) {
		List<StockMoneyFlowEntity> all = new ArrayList<>();
		for (int pn = 1; pn <= MAX_PAGES; pn++) {
			JSONObject page = fetchPage(pn);
			if (page == null) {
				break;
			}
			JSONArray diff = page.getJSONObject("data") != null
					? page.getJSONObject("data").getJSONArray("diff") : null;
			if (diff == null || diff.isEmpty()) {
				break;
			}
			for (int i = 0; i < diff.size(); i++) {
				JSONObject item = diff.getJSONObject(i);
				StockMoneyFlowEntity entity = mapRow(item, tradeDate);
				if (entity != null) {
					all.add(entity);
				}
			}
			int total = page.getJSONObject("data").getIntValue("total");
			if (pn * PAGE_SIZE >= total) {
				break;
			}
			try {
				Thread.sleep(PAGE_INTERVAL_MS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("资金流同步被中断");
				break;
			}
		}
		return all;
	}

	/**
	 * 新浪财经资金流兜底：遍历股票基础信息，按个股拉取指定交易日资金流
	 */
	private List<StockMoneyFlowEntity> fetchSinaAllFlows(String tradeDate) {
		List<StockBasicEntity> basics = stockBasicMapper.selectList(Wrappers.emptyWrapper());
		if (CollUtil.isEmpty(basics)) {
			throw new IllegalStateException("新浪资金流兜底失败：stock_basic 无股票基础信息");
		}
		List<StockMoneyFlowEntity> all = new ArrayList<>();
		int failCount = 0;
		for (int i = 0; i < basics.size(); i++) {
			StockBasicEntity basic = basics.get(i);
			try {
				StockMoneyFlowEntity row = fetchSinaOne(basic, tradeDate);
				if (row != null) {
					all.add(row);
				}
			}
			catch (Exception e) {
				failCount++;
				log.warn("新浪资金流 {} 同步失败: {}", basic.getTsCode(), e.getMessage());
			}
			if (SINA_REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(SINA_REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			if ((i + 1) % 500 == 0) {
				log.info("新浪资金流兜底进度: {}/{}, 成功={}, 失败={}", i + 1, basics.size(), all.size(), failCount);
			}
		}
		log.info("新浪财经资金流兜底完成, 股票数={}, 成功={}, 失败={}", basics.size(), all.size(), failCount);
		return all;
	}

	/**
	 * 拉取单只股票新浪财经资金流，匹配指定交易日
	 */
	private StockMoneyFlowEntity fetchSinaOne(StockBasicEntity basic, String tradeDate) {
		String tsCode = basic.getTsCode();
		String symbol = toSinaSymbol(tsCode);
		Map<String, Object> params = new HashMap<>(8);
		params.put("daima", symbol);
		params.put("page", 1);
		params.put("num", 20);
		params.put("sort", "opendate");
		params.put("asc", 0);

		String respBody = HttpRequest.get(SINA_MONEY_FLOW_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://finance.sina.com.cn")
				.timeout(15000)
				.execute()
				.body();
		if (StrUtil.isBlank(respBody)) {
			throw new IllegalStateException("新浪资金流接口响应为空: " + symbol);
		}
		JSONArray arr = JSON.parseArray(respBody);
		if (arr == null || arr.isEmpty()) {
			return null;
		}
		for (int i = 0; i < arr.size(); i++) {
			JSONObject item = arr.getJSONObject(i);
			String date = item.getString("opendate");
			if (StrUtil.isBlank(date)) {
				continue;
			}
			String day = date.replace("-", "");
			if (!tradeDate.equals(day)) {
				continue;
			}
			StockMoneyFlowEntity entity = new StockMoneyFlowEntity();
			entity.setTsCode(tsCode);
			entity.setName(basic.getName());
			entity.setIndustryName(basic.getIndustry());
			entity.setTradeDate(day);
			entity.setClose(decimal(item.getString("trade")));
			entity.setPctChg(decimal(item.getString("changepercent")));
			BigDecimal r0 = decimal(item.getString("r0_net"));
			BigDecimal r1 = decimal(item.getString("r1_net"));
			entity.setSuperLargeNet(r0);
			entity.setLargeNet(r1);
			entity.setMediumNet(decimal(item.getString("r2_net")));
			entity.setSmallNet(decimal(item.getString("r3_net")));
			if (r0 != null || r1 != null) {
				entity.setMainNetInflow((r0 == null ? BigDecimal.ZERO : r0).add(r1 == null ? BigDecimal.ZERO : r1));
			}
			entity.setMainNetPct(null);
			return entity;
		}
		return null;
	}

	/**
	 * TS代码转新浪代码：600519.SH -> sh600519
	 */
	private String toSinaSymbol(String tsCode) {
		String code = tsCode.split("\\.")[0];
		return tsCode.endsWith(".SH") ? "sh" + code : "sz" + code;
	}


	/**
	 * 单页请求
	 */
	private JSONObject fetchPage(int pn) {
		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				return doFetchPage(pn);
			}
			catch (Exception e) {
				last = e;
				log.warn("资金流分页请求失败(第 {}/{} 次), pn={}: {}", attempt, MAX_RETRY, pn, e.getMessage());
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
		throw new IllegalStateException("东方财富资金流分页接口请求失败: " + (last != null ? last.getMessage() : "unknown"), last);
	}


	private JSONObject doFetchPage(int pn) {
		Map<String, Object> params = Map.of(
				"pn", pn,
				"pz", PAGE_SIZE,
				"po", 1,
				"np", 1,
				"fltt", 2,
				"invt", 2,
				"fid", "f62",
				"fs", FS_A_SHARE,
				"fields", "f12,f14,f2,f3,f62,f66,f72,f78,f84,f184,f100");

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
			throw new IllegalStateException("东方财富资金流分页接口响应为空");
		}
		return JSON.parseObject(respBody);
	}

	/**
	 * 行映射：f12代码 f14名称 f2最新价 f3涨跌幅 f62主力净流入 f184主力净占比
	 * f66超大单 f72大单 f78中单 f84小单 f100所属行业
	 */
	private StockMoneyFlowEntity mapRow(JSONObject item, String tradeDate) {
		String code = item.getString("f12");
		if (StrUtil.isBlank(code)) {
			return null;
		}
		StockMoneyFlowEntity entity = new StockMoneyFlowEntity();
		entity.setTsCode(toTsCode(code));
		entity.setName(item.getString("f14"));
		entity.setIndustryName(item.getString("f100"));
		entity.setTradeDate(tradeDate);
		entity.setClose(decimal(item.getString("f2")));
		entity.setPctChg(decimal(item.getString("f3")));
		entity.setMainNetInflow(decimal(item.getString("f62")));
		entity.setMainNetPct(decimal(item.getString("f184")));
		entity.setSuperLargeNet(decimal(item.getString("f66")));
		entity.setLargeNet(decimal(item.getString("f72")));
		entity.setMediumNet(decimal(item.getString("f78")));
		entity.setSmallNet(decimal(item.getString("f84")));
		return entity;
	}

	/**
	 * 解析最新交易日：优先取指数日线最新交易日；无指数数据且当日为周末时返回 null（跳过同步）
	 */
	private String resolveTradeDate() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		if (latest != null && StrUtil.isNotBlank(latest.getTradeDate())) {
			return latest.getTradeDate();
		}
		LocalDate today = LocalDate.now();
		if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return null;
		}
		return today.format(BASIC_DATE);
	}

	/**
	 * 按唯一键 (ts_code, trade_date) 批量插入/更新（快照整体覆盖）
	 */
	private int upsertByUniqueKey(String tradeDate, List<StockMoneyFlowEntity> rows) {
		List<StockMoneyFlowEntity> existList = list(Wrappers.<StockMoneyFlowEntity>lambdaQuery()
				.select(StockMoneyFlowEntity::getId, StockMoneyFlowEntity::getTsCode)
				.eq(StockMoneyFlowEntity::getTradeDate, tradeDate));
		Map<String, StockMoneyFlowEntity> existMap = existList.stream()
				.collect(Collectors.toMap(StockMoneyFlowEntity::getTsCode, e -> e));

		List<StockMoneyFlowEntity> toInsert = new ArrayList<>();
		List<StockMoneyFlowEntity> toUpdate = new ArrayList<>();
		for (StockMoneyFlowEntity row : rows) {
			StockMoneyFlowEntity exist = existMap.get(row.getTsCode());
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
	 * 六位代码转 TS 代码（东财 fs 过滤已排除北交所，此处兜底处理）
	 */
	private String toTsCode(String code) {
		if (code.startsWith("6")) {
			return code + ".SH";
		}
		if (code.startsWith("0") || code.startsWith("3")) {
			return code + ".SZ";
		}
		if (code.startsWith("4") || code.startsWith("8")) {
			return code + ".BJ";
		}
		return code;
	}

	/**
	 * 数值解析：东财 fltt=2 已换算单位，"-" 表示停牌/无值
	 */
	private BigDecimal decimal(String value) {
		if (StrUtil.isBlank(value) || "-".equals(value)) {
			return null;
		}
		try {
			return new BigDecimal(value.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
