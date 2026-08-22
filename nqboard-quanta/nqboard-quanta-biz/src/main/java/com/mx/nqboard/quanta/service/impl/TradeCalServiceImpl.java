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
import com.mx.nqboard.quanta.api.entity.TradeCalEntity;
import com.mx.nqboard.quanta.config.QuantSyncLog;
import com.mx.nqboard.quanta.mapper.TradeCalMapper;
import com.mx.nqboard.quanta.service.TradeCalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 交易日历（tushare trade_cal） 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCalServiceImpl extends ServiceImpl<TradeCalMapper, TradeCalEntity>
		implements TradeCalService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";

	private static final String API_NAME_TRADE_CAL = "trade_cal";

	/**
	 * 单请求最大重试次数（网络抖动退避重试）
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 重试基础退避间隔（毫秒）
	 */
	private static final long RETRY_BASE_DELAY_MS = 1000L;

	/**
	 * 默认交易所（A股沪深交易日一致，取上交所口径）
	 */
	private static final String DEFAULT_EXCHANGE = "SSE";

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final TradeCalMapper tradeCalMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 同步区间：当年前后各 N 年（yml 配置 tushare.trade-cal.prefetch-years）
	 */
	@Value("${tushare.trade-cal.prefetch-years:1}")
	private int prefetchYears;

	/**
	 * 判断当日无日历数据时是否自动增量同步兜底（yml 配置 tushare.trade-cal.auto-sync-on-miss）
	 */
	@Value("${tushare.trade-cal.auto-sync-on-miss:true}")
	private boolean autoSyncOnMiss;

	/**
	 * 从 tushare 同步交易日历（区间 = 当年前后各 prefetch-years 年），按 (exchange, cal_date) upsert
	 */
	@Override
	@QuantSyncLog(type = "trade_cal", name = "交易日历同步")
	public SyncResult syncFromTushare() {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		int year = LocalDate.now().getYear();
		String start = LocalDate.of(year - prefetchYears, 1, 1).format(BASIC_DATE);
		String end = LocalDate.of(year + prefetchYears, 12, 31).format(BASIC_DATE);

		log.info("开始从 tushare 同步交易日历, exchange={}, 区间: {} ~ {}", DEFAULT_EXCHANGE, start, end);
		JSONObject data = postTushare(API_NAME_TRADE_CAL,
				Map.of("exchange", DEFAULT_EXCHANGE, "start_date", start, "end_date", end));
		if (data == null) {
			return SyncResult.builder().affected(0).successCount(0).totalCount(0).message("tushare 无返回数据").build();
		}
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");
		if (items == null || items.isEmpty()) {
			log.info("交易日历同步完成, 区间 {} ~ {} 内无数据", start, end);
			return SyncResult.builder().affected(0).successCount(0).totalCount(0)
					.syncRange(start + "~" + end).message("区间内无数据").build();
		}

		// 区间内已存在记录按 (exchange, cal_date) 建索引：命中走 update（保留原 id），其余批量插入，
		// 避免全量重跑时对唯一索引 uk_exchange_cal_date 的冲突
		List<TradeCalEntity> existList = list(Wrappers.<TradeCalEntity>lambdaQuery()
				.select(TradeCalEntity::getId, TradeCalEntity::getExchange, TradeCalEntity::getCalDate)
				.between(TradeCalEntity::getCalDate, start, end));
		Map<String, TradeCalEntity> existMap = new HashMap<>(existList.size() * 2);
		for (TradeCalEntity exist : existList) {
			existMap.putIfAbsent(key(exist.getExchange(), exist.getCalDate()), exist);
		}

		List<TradeCalEntity> toInsert = new ArrayList<>();
		List<TradeCalEntity> toUpdate = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			TradeCalEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity == null || StrUtil.isBlank(entity.getCalDate())) {
				continue;
			}
			TradeCalEntity exist = existMap.get(key(entity.getExchange(), entity.getCalDate()));
			if (exist != null) {
				entity.setId(exist.getId());
				toUpdate.add(entity);
			}
			else {
				toInsert.add(entity);
			}
		}
		int inserted = 0;
		int updated = 0;
		if (CollUtil.isNotEmpty(toInsert) && saveBatch(toInsert)) {
			inserted = toInsert.size();
		}
		if (CollUtil.isNotEmpty(toUpdate) && updateBatchById(toUpdate)) {
			updated = toUpdate.size();
		}
		log.info("从 tushare 同步交易日历完成, 区间: {} ~ {}, 新增 {} 条, 更新 {} 条", start, end, inserted, updated);
		return SyncResult.builder()
				.affected(inserted + updated)
				.successCount(inserted + updated)
				.totalCount(items.size())
				.syncRange(start + "~" + end)
				.message("新增 " + inserted + " 条, 更新 " + updated + " 条")
				.build();
	}

	/**
	 * 今天是否开盘（定时任务执行前的交易日闸门）
	 * <p>
	 * 优先查本地日历；当日无数据且开启 auto-sync-on-miss 时自动增量同步一次兜底；
	 * 仍无数据则退化为周末规则（周末必休市，工作日按开盘处理，避免同步失败阻断任务）
	 * </p>
	 */
	@Override
	public boolean isOpenToday() {
		String today = LocalDate.now().format(BASIC_DATE);
		TradeCalEntity row = queryByDate(today);
		if (row == null && autoSyncOnMiss) {
			try {
				syncFromTushare();
				row = queryByDate(today);
			}
			catch (Exception e) {
				log.warn("交易日历自动同步失败，按兜底规则判断今日是否开盘: {}", e.getMessage());
			}
		}
		if (row == null) {
			DayOfWeek dow = LocalDate.now().getDayOfWeek();
			boolean open = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
			log.warn("交易日历无 {} 数据，按{}处理", today, open ? "开盘" : "休市");
			return open;
		}
		return "1".equals(row.getIsOpen());
	}

	/**
	 * 指定日期是否为交易日
	 */
	@Override
	public boolean isTradeDate(String date) {
		if (StrUtil.isBlank(date)) {
			return false;
		}
		TradeCalEntity row = queryByDate(date);
		return row != null && "1".equals(row.getIsOpen());
	}

	/**
	 * 按日期查询日历（默认上交所口径，唯一键 exchange + cal_date）
	 */
	private TradeCalEntity queryByDate(String date) {
		return getOne(Wrappers.<TradeCalEntity>lambdaQuery()
				.eq(TradeCalEntity::getExchange, DEFAULT_EXCHANGE)
				.eq(TradeCalEntity::getCalDate, date), false);
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
	 * 按 fields 顺序把 items 行映射为实体
	 */
	private TradeCalEntity mapRow(List<String> fields, JSONArray item) {
		TradeCalEntity entity = new TradeCalEntity();
		boolean hasValue = false;
		for (int i = 0; i < fields.size(); i++) {
			String value = item.getString(i);
			if (value != null) {
				hasValue = true;
			}
			assignField(entity, fields.get(i), value);
		}
		return hasValue ? entity : null;
	}

	/**
	 * 按字段名填充实体属性
	 */
	private void assignField(TradeCalEntity entity, String field, String value) {
		switch (field) {
			case "exchange" -> entity.setExchange(value);
			case "cal_date" -> entity.setCalDate(value);
			case "is_open" -> entity.setIsOpen(value);
			case "pretrade_date" -> entity.setPretradeDate(value);
			default -> log.debug("忽略未知字段: {}", field);
		}
	}

	/**
	 * 唯一键 (exchange, cal_date)
	 */
	private String key(String exchange, String calDate) {
		return exchange + "_" + calDate;
	}

}
