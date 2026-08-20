package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Tushare股票基础信息 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockBasicServiceImpl extends ServiceImpl<StockBasicMapper, StockBasicEntity>
		implements StockBasicService {

	private static final String TUSHARE_URL = "https://api.tushare.pro";
	private static final String API_NAME_STOCK_BASIC = "stock_basic";

	/**
	 * 默认同步市场
	 * 			主板,创业板,科创板
	 */
	private static final String DEFAULT_MARKET = "主板";

	private final StockBasicMapper stockBasicMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:}")
	private String token;

	/**
	 * 无参重载，便于 Quartz 定时任务在未配置参数时直接调用
	 * @return 同步成功的条数
	 */
	public int syncFromTushare() {
		return syncFromTushare(DEFAULT_MARKET);
	}

	/**
	 * 从 tushare 同步股票基础信息（按市场），按 ts_code 唯一键 upsert
	 */
	@Override
	public int syncFromTushare(String market) {
		if (StrUtil.isBlank(token)) {
			throw new IllegalStateException("tushare token 未配置，请在 Nacos 配置或环境变量中设置 tushare.token");
		}
		// 组装请求体（fastjson 序列化）
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME_STOCK_BASIC);
		params.put("token", token);
		params.put("params", Map.of("market", StrUtil.blankToDefault(market, "")));

		log.info("开始从 tushare 同步股票基础信息, market={}", market);
		// hutool 发起 JSON POST 请求
		String respBody = HttpRequest.post(TUSHARE_URL)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(params))
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getIntValue("code") != 0) {
			String msg = body != null ? body.getString("msg") : "空响应";
			throw new IllegalStateException("tushare 接口调用失败: " + msg);
		}

		JSONObject data = body.getJSONObject("data");
		List<String> fields = data.getJSONArray("fields").toJavaList(String.class);
		JSONArray items = data.getJSONArray("items");

		List<StockBasicEntity> entities = new ArrayList<>();
		int inserted = 0;
		for (int i = 0; i < items.size(); i++) {
			StockBasicEntity entity = mapRow(fields, items.getJSONArray(i));
			if (entity == null || StrUtil.isBlank(entity.getTsCode())) {
				continue;
			}
			if (saveOrUpdateByTsCode(entity)) {
				inserted++;
			}
			entities.add(entity);
		}
		log.info("从 tushare 同步完成, market={}, 共处理 {} 条", market, entities.size());
		return inserted;
	}

	/**
	 * 按 ts_code 判断：存在则更新，不存在则新增
	 */
	private boolean saveOrUpdateByTsCode(StockBasicEntity entity) {
		StockBasicEntity exist = getOne(Wrappers.<StockBasicEntity>lambdaQuery()
				.eq(StockBasicEntity::getTsCode, entity.getTsCode()), false);
		if (exist != null) {
			entity.setId(exist.getId());
			entity.setUpdateTime(LocalDateTime.now());
			return updateById(entity);
		}
		entity.setCreateTime(LocalDateTime.now());
		return save(entity);
	}

	/**
	 * 按 fields 顺序把 items 行映射为实体
	 */
	private StockBasicEntity mapRow(List<String> fields, JSONArray item) {
		StockBasicEntity entity = new StockBasicEntity();
		boolean hasValue = false;
		for (int i = 0; i < fields.size(); i++) {
			String field = fields.get(i);
			String value = item.getString(i);
			if (value != null) {
				hasValue = true;
			}
			assignField(entity, field, value);
		}
		return hasValue ? entity : null;
	}

	/**
	 * 按字段名填充实体属性
	 */
	private void assignField(StockBasicEntity entity, String field, String value) {
		switch (field) {
			case "ts_code" -> entity.setTsCode(value);
			case "symbol" -> entity.setSymbol(value);
			case "name" -> entity.setName(value);
			case "area" -> entity.setArea(value);
			case "industry" -> entity.setIndustry(value);
			case "cnspell" -> entity.setCnspell(value);
			case "market" -> entity.setMarket(value);
			case "list_date" -> entity.setListDate(value);
			case "act_name" -> entity.setActName(value);
			case "act_ent_type" -> entity.setActEntType(value);
			default -> log.debug("忽略未知字段: {}", field);
		}
	}

}
