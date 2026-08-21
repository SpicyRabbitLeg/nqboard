package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockMotHolderEntity;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockMotHolderMapper;
import com.mx.nqboard.quanta.service.StockMotHolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
 * Tushare 股东增减持表 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMotHolderServiceImpl extends ServiceImpl<StockMotHolderMapper, StockMotHolderEntity>
		implements StockMotHolderService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";
	private static final String API_NAME_MOT_HOLDER = "stk_holdertrade";

	/**
	 * 全量同步起始日期：2026-08-01
	 */
	private static final LocalDate FULL_SYNC_START = LocalDate.of(2026, 8, 1);

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
	 * stk_holdertrade 接口限频 100 次/分钟（600ms/次），不能与日线共用节奏，故直接写死，留安全余量
	 */
	private static final long REQUEST_INTERVAL_MS = 800L;

	private final StockMotHolderMapper stockMotHolderMapper;

	private final StockBasicMapper stockBasicMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 复用日线同步市场过滤配置（yml 配置 tushare.daily.market）
	 */
	@Value("${tushare.daily.market:全部}")
	private String syncMarket;

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
	 * 从 tushare 同步股东增减持
	 * <p>
	 * 流程：读取 stock_basic 全量股票代码（按市场过滤）→ 逐股票调用 tushare stk_holdertrade 接口
	 * → 按唯一键 (ts_code, ann_date, holder_name) 批量插入/更新
	 */
	@Override
	public int syncFromTushare(String market, Boolean full) {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		// 入参优先，未传则取 yml 配置（复用日线配置项）
		String syncMarket = StrUtil.blankToDefault(market, this.syncMarket);
		boolean syncFull = full != null ? full : this.syncFull;

		LocalDate endDate = LocalDate.now();
		LocalDate startDate = syncFull ? FULL_SYNC_START : endDate;
		String start = startDate.format(BASIC_DATE);
		String end = endDate.format(BASIC_DATE);
		log.info("开始从 tushare 同步股东增减持, market={}, full={}, 区间: {} ~ {}", syncMarket, syncFull, start, end);

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

		// 2. 逐股票拉取股东增减持并落库
		int total = 0;
		int failCount = 0;
		for (int i = 0; i < tsCodes.size(); i++) {
			String tsCode = tsCodes.get(i);
			try {
				List<StockMotHolderEntity> rows = fetchHolders(tsCode, start, end);
				int affected = upsertByUniqueKey(tsCode, rows);
				total += affected;
				if ((i + 1) % 200 == 0 || i == tsCodes.size() - 1) {
					log.info("股东增减持同步进度: {}/{}, 累计影响 {} 行, 失败 {} 只", i + 1, tsCodes.size(), total, failCount);
				}
			}
			catch (Exception e) {
				failCount++;
				log.error("同步 {} 股东增减持失败: {}", tsCode, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("股东增减持同步被中断");
					break;
				}
			}
		}
		log.info("从 tushare 同步股东增减持完成, market={}, full={}, 股票数={}, 累计影响 {} 行, 失败 {} 只",
				syncMarket, syncFull, tsCodes.size(), total, failCount);
		return total;
	}

	/**
	 * 调用 tushare stk_holdertrade 接口拉取单只股票股东增减持
	 * <p>
	 * start_date/end_date 为公告日期范围：全量=20260801 至今天，增量=仅今天
	 */
	private List<StockMotHolderEntity> fetchHolders(String tsCode, String start, String end) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME_MOT_HOLDER);
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
			throw new IllegalStateException("tushare stk_holdertrade 接口调用失败: " + msg);
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
		List<StockMotHolderEntity> list = new ArrayList<>(items.size());
		for (int i = 0; i < items.size(); i++) {
			StockMotHolderEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity != null && StrUtil.isNotBlank(entity.getTsCode()) && StrUtil.isNotBlank(entity.getAnnDate())
					&& StrUtil.isNotBlank(entity.getHolderName())) {
				list.add(entity);
			}
		}
		return list;
	}

	/**
	 * 按唯一键 (ts_code, ann_date, holder_name) 批量插入/更新
	 * <p>
	 * 先查该股票已有记录，已存在的走 update（保留原 id），其余走批量插入，
	 * 避免全量重跑时对唯一索引 uk_mot_holder_unique 的冲突
	 */
	private int upsertByUniqueKey(String tsCode, List<StockMotHolderEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		List<StockMotHolderEntity> existList = list(Wrappers.<StockMotHolderEntity>lambdaQuery()
				.select(StockMotHolderEntity::getId, StockMotHolderEntity::getAnnDate, StockMotHolderEntity::getHolderName)
				.eq(StockMotHolderEntity::getTsCode, tsCode));
		Map<String, StockMotHolderEntity> existMap = existList.stream()
				.collect(Collectors.toMap(e -> e.getAnnDate() + "|" + e.getHolderName(), e -> e));

		List<StockMotHolderEntity> toInsert = new ArrayList<>();
		List<StockMotHolderEntity> toUpdate = new ArrayList<>();
		for (StockMotHolderEntity row : rows) {
			StockMotHolderEntity exist = existMap.get(row.getAnnDate() + "|" + row.getHolderName());
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
	private StockMotHolderEntity mapRow(List<String> fields, JSONArray item) {
		StockMotHolderEntity entity = new StockMotHolderEntity();
		for (int i = 0; i < fields.size(); i++) {
			switch (fields.get(i)) {
				case "ts_code" -> entity.setTsCode(strVal(item, i));
				case "ann_date" -> entity.setAnnDate(strVal(item, i));
				case "holder_name" -> entity.setHolderName(strVal(item, i));
				case "holder_type" -> entity.setHolderType(strVal(item, i));
				// tushare 原字段 in_de，落库列名 in_des
				case "in_de" -> entity.setInDes(strVal(item, i));
				case "change_vol" -> entity.setChangeVol(decimalVal(item, i));
				case "change_ratio" -> entity.setChangeRatio(decimalVal(item, i));
				case "after_share" -> entity.setAfterShare(decimalVal(item, i));
				case "after_ratio" -> entity.setAfterRatio(decimalVal(item, i));
				case "avg_price" -> entity.setAvgPrice(decimalVal(item, i));
				case "total_share" -> entity.setTotalShare(decimalVal(item, i));
				case "begin_date" -> entity.setBeginDate(strVal(item, i));
				case "close_date" -> entity.setCloseDate(strVal(item, i));
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

	/**
	 * 是否同步全部市场（配置为空或"全部"时不过滤）
	 */
	private boolean isAllMarket(String market) {
		return StrUtil.isBlank(market) || MARKET_ALL.equals(market) || "all".equalsIgnoreCase(market);
	}

}
