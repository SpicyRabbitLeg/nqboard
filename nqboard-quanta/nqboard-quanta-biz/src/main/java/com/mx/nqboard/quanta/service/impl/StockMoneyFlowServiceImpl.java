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
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.service.StockMoneyFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 大盘基准指数（用于解析最新交易日，避免非交易日写入脏数据）
	 */
	private static final String BENCHMARK_INDEX = "sh000300";

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	@Override
	public int syncFromEastMoney() {
		String tradeDate = resolveTradeDate();
		if (tradeDate == null) {
			log.warn("资金流同步跳过：无法解析最新交易日（指数日线无数据且当日为非交易日）");
			return 0;
		}
		log.info("开始从 东方财富 同步个股主力资金流, tradeDate={}", tradeDate);

		List<StockMoneyFlowEntity> rows = fetchAllFlows(tradeDate);
		if (CollUtil.isEmpty(rows)) {
			log.warn("东方财富 资金流接口无数据返回");
			return 0;
		}
		int affected = upsertByUniqueKey(tradeDate, rows);
		log.info("从 东方财富 同步个股主力资金流完成, tradeDate={}, 股票数={}, 影响 {} 行", tradeDate, rows.size(), affected);
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
	 * 单页请求
	 */
	private JSONObject fetchPage(int pn) {
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
				.timeout(15000)
				.execute()
				.body();
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
