package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
	 */
	private static final String DEFAULT_MARKET = "主板";

	private final StockBasicMapper stockBasicMapper;

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	/**
	 * tushare token（Nacos 配置 + 环境变量注入，禁止硬编码）
	 */
	@Value("${tushare.token:uyuibwqjgeuq}")
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
		// 组装请求体
		Map<String, Object> params = new HashMap<>(4);
		params.put("api_name", API_NAME_STOCK_BASIC);
		params.put("token", token);
		params.put("params", Map.of("market", StrUtil.blankToDefault(market, "")));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

		log.info("开始从 tushare 同步股票基础信息, market={}", market);
		ResponseEntity<JsonNode> response = restTemplate.postForEntity(TUSHARE_URL, request, JsonNode.class);
		JsonNode body = response.getBody();
		if (body == null || body.get("code").asInt() != 0) {
			String msg = body != null ? body.has("msg") ? body.get("msg").asText() : body.toString() : "空响应";
			throw new IllegalStateException("tushare 接口调用失败: " + msg);
		}

		JsonNode dataNode = body.get("data");
		List<String> fields = objectMapper.convertValue(dataNode.get("fields"), new TypeReference<>() {});
		JsonNode itemsNode = dataNode.get("items");

		List<StockBasicEntity> entities = new ArrayList<>();
		int inserted = 0;
		for (JsonNode item : itemsNode) {
			StockBasicEntity entity = mapRow(fields, item);
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
	private StockBasicEntity mapRow(List<String> fields, JsonNode item) {
		StockBasicEntity entity = new StockBasicEntity();
		boolean hasValue = false;
		for (int i = 0; i < fields.size(); i++) {
			String field = fields.get(i);
			String value = item.has(i) && !item.get(i).isNull() ? item.get(i).asText() : null;
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
