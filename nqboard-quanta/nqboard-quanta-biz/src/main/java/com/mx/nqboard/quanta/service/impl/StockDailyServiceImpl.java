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
	 * 全量同步起始日期：2024-01-01
	 */
	private static final LocalDate FULL_SYNC_START = LocalDate.of(2024, 1, 1);

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 市场过滤值：全部
	 */
	private static final String MARKET_ALL = "全部";

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

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
	 * 相邻股票请求间隔毫秒（yml 配置 tushare.daily.delay-ms，防止触发 tushare 频率限制）
	 */
	@Value("${tushare.daily.delay-ms:200}")
	private long delayMs;

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
		LocalDate startDate = syncFull ? FULL_SYNC_START : endDate;
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
			if (delayMs > 0) {
				try {
					Thread.sleep(delayMs);
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
		return total;
	}

	/**
	 * 调用 tushare daily 接口拉取单只股票日线
	 */
	private List<StockDailyEntity> fetchDaily(String tsCode, String start, String end) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME_DAILY);
		params.put("token", token);
		params.put("params", Map.of("ts_code", tsCode, "start_date", start, "end_date", end));

		String respBody = HttpRequest.post(TUSHARE_URL)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(params))
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getIntValue("code") != 0) {
			String msg = body != null ? body.getString("msg") : "空响应";
			throw new IllegalStateException("tushare daily 接口调用失败: " + msg);
		}

		JSONObject data = body.getJSONObject("data");
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
