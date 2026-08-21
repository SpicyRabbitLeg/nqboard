package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.mx.nqboard.quanta.mapper.StockTopListMapper;
import com.mx.nqboard.quanta.service.StockTopListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
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
 * Tushare top_list 龙虎榜每日明细 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTopListServiceImpl extends ServiceImpl<StockTopListMapper, StockTopListEntity>
		implements StockTopListService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";
	private static final String API_NAME_TOP_LIST = "top_list";

	/**
	 * 全量同步起始日期：2026-01-01
	 */
	private static final LocalDate FULL_SYNC_START = LocalDate.of(2026, 1, 1);

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 相邻日期请求间隔（毫秒）
	 * <p>
	 * top_list 接口按 200 次/分钟档预留（300ms/次），写死留安全余量，可按积分档位调整
	 */
	private static final long REQUEST_INTERVAL_MS = 500L;

	private final StockTopListMapper stockTopListMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 复用日线全量同步开关（yml 配置 tushare.daily.full）
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
	 * 从 tushare 同步龙虎榜每日明细（按交易日期遍历）
	 */
	@Override
	public int syncFromTushare(String tradeDate, Boolean full) {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		boolean syncFull = full != null ? full : this.syncFull;

		LocalDate endDate = LocalDate.now();
		LocalDate startDate = syncFull ? FULL_SYNC_START : endDate;
		String start = startDate.format(BASIC_DATE);
		String end = endDate.format(BASIC_DATE);
		log.info("开始从 tushare 同步龙虎榜, tradeDate={}, full={}, 区间: {} ~ {}", tradeDate, syncFull, start, end);

		// 指定单日则只同步该日，否则按区间逐日遍历（跳过周末）
		List<String> dates;
		if (StrUtil.isNotBlank(tradeDate)) {
			dates = List.of(tradeDate);
		}
		else {
			dates = new ArrayList<>();
			for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
				if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
					continue;
				}
				dates.add(d.format(BASIC_DATE));
			}
		}
		log.info("待同步日期数: {}（按全量起始日期计算）", dates.size());

		int total = 0;
		int failCount = 0;
		for (int i = 0; i < dates.size(); i++) {
			String date = dates.get(i);
			try {
				List<StockTopListEntity> rows = fetchByDate(date);
				int affected = upsertByDate(date, rows);
				total += affected;
				if ((i + 1) % 30 == 0 || i == dates.size() - 1) {
					log.info("龙虎榜同步进度: {}/{}, 累计影响 {} 行, 失败 {} 天", i + 1, dates.size(), total, failCount);
				}
			}
			catch (Exception e) {
				failCount++;
				log.error("同步 {} 龙虎榜失败: {}", date, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("龙虎榜同步被中断");
					break;
				}
			}
		}
		log.info("从 tushare 同步龙虎榜完成, 日期数={}, 累计影响 {} 行, 失败 {} 天", dates.size(), total, failCount);
		return total;
	}

	/**
	 * 调用 tushare top_list 接口拉取单日龙虎榜明细
	 */
	private List<StockTopListEntity> fetchByDate(String tradeDate) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME_TOP_LIST);
		params.put("token", token);
		params.put("params", Map.of("trade_date", tradeDate));

		String respBody = HttpRequest.post(TUSHARE_URL)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(params))
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getIntValue("code") != 0) {
			String msg = body != null ? body.getString("msg") : "空响应";
			throw new IllegalStateException("tushare top_list 接口调用失败: " + msg);
		}

		JSONObject data = body.getJSONObject("data");
		if (data == null) {
			return Collections.emptyList();
		}
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");
		if (items == null || items.isEmpty()) {
			// 非交易日或当日无上榜
			log.debug("{} 无龙虎榜数据", tradeDate);
			return Collections.emptyList();
		}
		List<StockTopListEntity> list = new ArrayList<>(items.size());
		for (int i = 0; i < items.size(); i++) {
			StockTopListEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity != null && StrUtil.isNotBlank(entity.getTsCode()) && StrUtil.isNotBlank(entity.getTradeDate())) {
				list.add(entity);
			}
		}
		return list;
	}

	/**
	 * 按唯一键 (trade_date, ts_code) 批量插入/更新
	 * <p>
	 * 单次调用覆盖一个交易日，按该日已有记录分区，避免对唯一索引 idx_trade_ts 的冲突
	 */
	private int upsertByDate(String tradeDate, List<StockTopListEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		// Tushare 返回的数据中同一股票同一天可能有多条，先按 ts_code 去重，避免唯一键冲突
		Map<String, StockTopListEntity> uniqueRows = new HashMap<>();
		for (StockTopListEntity row : rows) {
			uniqueRows.putIfAbsent(row.getTsCode(), row);
		}
		rows = new ArrayList<>(uniqueRows.values());
		List<StockTopListEntity> existList = list(Wrappers.<StockTopListEntity>lambdaQuery()
				.select(StockTopListEntity::getId, StockTopListEntity::getTsCode)
				.eq(StockTopListEntity::getTradeDate, tradeDate));
		Map<String, Long> existMap = existList.stream()
				.collect(Collectors.toMap(StockTopListEntity::getTsCode, StockTopListEntity::getId));

		List<StockTopListEntity> toInsert = new ArrayList<>();
		List<StockTopListEntity> toUpdate = new ArrayList<>();
		for (StockTopListEntity row : rows) {
			Long id = existMap.get(row.getTsCode());
			if (id == null) {
				toInsert.add(row);
			}
			else {
				row.setId(id);
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
	private StockTopListEntity mapRow(List<String> fields, JSONArray item) {
		StockTopListEntity entity = new StockTopListEntity();
		for (int i = 0; i < fields.size(); i++) {
			switch (fields.get(i)) {
				case "trade_date" -> entity.setTradeDate(strVal(item, i));
				case "ts_code" -> entity.setTsCode(strVal(item, i));
				case "name" -> entity.setName(strVal(item, i));
				case "close" -> entity.setClose(decimalVal(item, i));
				case "pct_change" -> entity.setPctChange(decimalVal(item, i));
				case "turnover_rate" -> entity.setTurnoverRate(decimalVal(item, i));
				case "amount" -> entity.setAmount(decimalVal(item, i));
				case "l_sell" -> entity.setLSell(decimalVal(item, i));
				case "l_buy" -> entity.setLBuy(decimalVal(item, i));
				case "l_amount" -> entity.setLAmount(decimalVal(item, i));
				case "net_amount" -> entity.setNetAmount(decimalVal(item, i));
				case "net_rate" -> entity.setNetRate(decimalVal(item, i));
				case "amount_rate" -> entity.setAmountRate(decimalVal(item, i));
				case "float_values" -> entity.setFloatValues(decimalVal(item, i));
				case "reason" -> entity.setReason(strVal(item, i));
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
	 * 高精度数值取值（decimal 列），JSON null / "null" / 空串 转为 null
	 */
	private BigDecimal decimalVal(JSONArray item, int i) {
		String s = item.getString(i);
		if (StrUtil.isBlank(s) || "null".equals(s)) {
			return null;
		}
		return new BigDecimal(s.trim());
	}

}
