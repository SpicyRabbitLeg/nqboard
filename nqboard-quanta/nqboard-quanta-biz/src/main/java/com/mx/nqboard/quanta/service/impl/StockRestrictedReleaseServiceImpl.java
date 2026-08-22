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
import com.mx.nqboard.quanta.api.entity.StockRestrictedReleaseEntity;
import com.mx.nqboard.quanta.config.QuantSyncLog;
import com.mx.nqboard.quanta.mapper.StockRestrictedReleaseMapper;
import com.mx.nqboard.quanta.service.StockRestrictedReleaseService;
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
import java.util.stream.Collectors;

/**
 * <p>
 * 限售解禁 服务实现类
 * </p>
 * <p>
 * 数据来源：tushare share_float 接口（按公告日 ann_date 拉取全市场）。
 * 增量模式单次请求当日公告；全量模式自回补起始日逐个交易日拉取
 * （share_float 无全局无参全量接口，按日分片最稳）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRestrictedReleaseServiceImpl extends ServiceImpl<StockRestrictedReleaseMapper, StockRestrictedReleaseEntity>
		implements StockRestrictedReleaseService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";

	private static final String API_NAME = "share_float";

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 相邻日期请求间隔（毫秒），share_float 限频低于 daily，留安全余量
	 */
	private static final long REQUEST_INTERVAL_MS = 500L;

	/**
	 * 全量回补默认天数（yml 可覆盖 restricted-release.lookback-days）
	 */
	private static final int DEFAULT_LOOKBACK_DAYS = 90;

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	private final StockRestrictedReleaseMapper stockRestrictedReleaseMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 全量回补天数（yml 配置 restricted-release.lookback-days）
	 */
	@Value("${restricted-release.lookback-days:90}")
	private int lookbackDays;

	@Override
	@QuantSyncLog(type = "restricted_release", name = "限售解禁同步")
	public SyncResult syncFromTushare() {
		return syncFromTushare(null);
	}

	@Override
	@QuantSyncLog(type = "restricted_release", name = "限售解禁同步")
	public SyncResult syncFromTushare(Boolean full) {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		LocalDate today = LocalDate.now();
		LocalDate startDate = Boolean.TRUE.equals(full)
				? today.minusDays(lookbackDays > 0 ? lookbackDays : DEFAULT_LOOKBACK_DAYS)
				: today;
		log.info("开始从 tushare 同步限售解禁, full={}, 区间: {} ~ {}", full, startDate.format(BASIC_DATE), today.format(BASIC_DATE));

		int total = 0;
		int failCount = 0;
		for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
			// 周末无公告，跳过（节省调用）
			if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				continue;
			}
			String annDate = date.format(BASIC_DATE);
			try {
				List<StockRestrictedReleaseEntity> rows = fetchByAnnDate(annDate);
				total += upsertByUniqueKey(rows);
			}
			catch (Exception e) {
				failCount++;
				log.error("同步 {} 限售解禁失败: {}", annDate, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("限售解禁同步被中断");
					break;
				}
			}
		}
		log.info("从 tushare 同步限售解禁完成, 区间 {} ~ {}, 累计影响 {} 行, 失败 {} 天",
				startDate.format(BASIC_DATE), today.format(BASIC_DATE), total, failCount);
		return SyncResult.builder()
				.affected(total)
				.successCount(total)
				.failCount(failCount)
				.syncRange(startDate.format(BASIC_DATE) + "~" + today.format(BASIC_DATE))
				.message("影响 " + total + " 行, 失败 " + failCount + " 个公告日")
				.build();
	}

	/**
	 * 调用 tushare share_float 接口按公告日拉取
	 */
	private List<StockRestrictedReleaseEntity> fetchByAnnDate(String annDate) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME);
		params.put("token", token);
		params.put("params", Map.of("ann_date", annDate));

		String respBody = HttpRequest.post(TUSHARE_URL)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(params))
				.timeout(30000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getIntValue("code") != 0) {
			String msg = body != null ? body.getString("msg") : "空响应";
			throw new IllegalStateException("tushare share_float 接口调用失败: " + msg);
		}

		JSONObject data = body.getJSONObject("data");
		if (data == null) {
			return new ArrayList<>();
		}
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");
		if (items == null || items.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockRestrictedReleaseEntity> list = new ArrayList<>(items.size());
		for (int i = 0; i < items.size(); i++) {
			StockRestrictedReleaseEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity != null && StrUtil.isNotBlank(entity.getTsCode())
					&& StrUtil.isNotBlank(entity.getFloatDate())) {
				list.add(entity);
			}
		}
		return list;
	}

	/**
	 * 按 fields 顺序把 items 行映射为实体
	 */
	private StockRestrictedReleaseEntity mapRow(List<String> fields, JSONArray item) {
		StockRestrictedReleaseEntity entity = new StockRestrictedReleaseEntity();
		for (int i = 0; i < fields.size(); i++) {
			switch (fields.get(i)) {
				case "ts_code" -> entity.setTsCode(strVal(item, i));
				case "ann_date" -> entity.setAnnDate(strVal(item, i));
				case "float_date" -> entity.setFloatDate(strVal(item, i));
				case "float_share" -> entity.setFloatShare(decimalVal(item, i));
				case "float_ratio" -> entity.setFloatRatio(decimalVal(item, i));
				case "holder_name" -> entity.setHolderName(strVal(item, i));
				default -> log.debug("忽略未知字段: {}", fields.get(i));
			}
		}
		return entity;
	}

	/**
	 * 按唯一键 (ts_code, float_date, holder_name) 批量插入/更新
	 */
	private int upsertByUniqueKey(List<StockRestrictedReleaseEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		// 同一批数据内部按唯一键去重（tushare 可能重复返回）
		Map<String, StockRestrictedReleaseEntity> unique = new HashMap<>();
		for (StockRestrictedReleaseEntity row : rows) {
			unique.put(uk(row), row);
		}
		List<String> tsCodes = unique.values().stream()
				.map(StockRestrictedReleaseEntity::getTsCode).distinct().toList();
		Map<String, List<StockRestrictedReleaseEntity>> existByTs = list(Wrappers.<StockRestrictedReleaseEntity>lambdaQuery()
				.in(StockRestrictedReleaseEntity::getTsCode, tsCodes))
				.stream()
				.collect(Collectors.groupingBy(StockRestrictedReleaseEntity::getTsCode));
		Map<String, Long> existUkMap = new HashMap<>();
		existByTs.forEach((ts, entities) -> entities.forEach(e -> existUkMap.put(uk(e), e.getId())));

		List<StockRestrictedReleaseEntity> toInsert = new ArrayList<>();
		List<StockRestrictedReleaseEntity> toUpdate = new ArrayList<>();
		unique.forEach((k, row) -> {
			Long existId = existUkMap.get(k);
			if (existId == null) {
				toInsert.add(row);
			}
			else {
				row.setId(existId);
				toUpdate.add(row);
			}
		});
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert, BATCH_SIZE);
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, BATCH_SIZE);
		}
		return unique.size();
	}

	private String uk(StockRestrictedReleaseEntity e) {
		// holder_name 可能为空，用空串兜底保证唯一键稳定
		return e.getTsCode() + "|" + e.getFloatDate() + "|" + StrUtil.nullToEmpty(e.getHolderName());
	}

	private String strVal(JSONArray item, int i) {
		String s = item.getString(i);
		return StrUtil.isBlank(s) || "null".equals(s) ? null : s;
	}

	private java.math.BigDecimal decimalVal(JSONArray item, int i) {
		String s = item.getString(i);
		if (StrUtil.isBlank(s) || "null".equals(s)) {
			return null;
		}
		try {
			return new java.math.BigDecimal(s.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
