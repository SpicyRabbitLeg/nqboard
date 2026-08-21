package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.api.entity.StockMotAnnNewsEntity;
import com.mx.nqboard.quanta.api.entity.StockMotHolderCountEntity;
import com.mx.nqboard.quanta.api.entity.StockMotHolderEntity;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.mapper.StockMotAnnNewsMapper;
import com.mx.nqboard.quanta.mapper.StockMotHolderCountMapper;
import com.mx.nqboard.quanta.mapper.StockMotHolderMapper;
import com.mx.nqboard.quanta.mapper.StockScreenResultMapper;
import com.mx.nqboard.quanta.mapper.StockTopListMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Dify Workflow 数据供给接口（供 Dify HTTP 节点调用）
 * </p>
 * <p>
 * 设计要点：
 * <ul>
 * <li>返回裸 JSON（不包 R&lt;T&gt;），减少 LLM 解析噪音</li>
 * <li>每响应带 _meta（数据截止日/条数/是否完整），让 Agent 对数据缺失诚实</li>
 * <li>静态 Token 鉴权（X-Dify-Token，yml 配置 dify.tools-token；未配置时放行便于本地调试）</li>
 * <li>GET /dify/openapi.json 输出 OpenAPI 3 规范，可直接导入 Dify 自定义工具</li>
 * <li>网关需放行 /quanta/dify/** 路径（Nacos 网关路由已覆盖 /quanta/**，鉴权由本 Token 承担）</li>
 * </ul>
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dify")
@Tag(description = "dify", name = "Dify数据供给接口")
public class DifyToolsController {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String BENCHMARK_INDEX = "sh000300";

	private final StockDailyMapper stockDailyMapper;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	private final StockIndustryDailyMapper stockIndustryDailyMapper;

	private final StockMoneyFlowMapper stockMoneyFlowMapper;

	private final StockTopListMapper stockTopListMapper;

	private final StockMotAnnNewsMapper stockMotAnnNewsMapper;

	private final StockMotHolderMapper stockMotHolderMapper;

	private final StockMotHolderCountMapper stockMotHolderCountMapper;

	private final StockScreenResultMapper stockScreenResultMapper;

	/**
	 * Dify 回调鉴权 Token（yml 配置 dify.tools-token，为空放行）
	 */
	@Value("${dify.tools-token:}")
	private String toolsToken;

	/**
	 * 技术面特征 + 筛选打分（Dify 技术面分析师数据源）
	 */
	@Operation(summary = "技术面特征与打分", description = "特征向量 + 三层筛选打分结果")
	@GetMapping("/technicals")
	public String technicals(@RequestParam("tsCode") String tsCode,
			@RequestParam(value = "date", required = false) String date) {
		checkToken();
		String tradeDate = resolveDate(date);
		StockScreenResultEntity screen = stockScreenResultMapper.selectOne(
				Wrappers.<StockScreenResultEntity>lambdaQuery()
						.eq(StockScreenResultEntity::getTsCode, tsCode)
						.eq(StockScreenResultEntity::getTradeDate, tradeDate)
						.last("limit 1"));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		resp.put("trade_date", tradeDate);
		if (screen != null) {
			resp.put("screen_score", screen.getScreenScore());
			resp.put("pattern", screen.getPattern());
			resp.put("passed", "1".equals(screen.getPassed()));
			if (StrUtil.isNotBlank(screen.getMetrics())) {
				resp.put("metrics", JSON.parseObject(screen.getMetrics()));
			}
		}
		else {
			resp.put("screen_score", null);
			resp.put("note", "该日无筛选记录");
		}
		return meta(resp, tradeDate, screen != null);
	}

	/**
	 * 龙虎榜记录（Dify 龙虎榜分析师数据源）
	 */
	@Operation(summary = "龙虎榜记录", description = "近N个交易日上榜记录与净买额")
	@GetMapping("/dragon-tiger")
	public String dragonTiger(@RequestParam("tsCode") String tsCode,
			@RequestParam(value = "days", required = false, defaultValue = "5") int days) {
		checkToken();
		String tradeDate = latestTradeDate();
		String start = minusDays(tradeDate, Math.max(days, 1) * 3);
		List<StockTopListEntity> rows = stockTopListMapper.selectList(
				Wrappers.<StockTopListEntity>lambdaQuery()
						.eq(StockTopListEntity::getTsCode, tsCode)
						.ge(StockTopListEntity::getTradeDate, start)
						.le(StockTopListEntity::getTradeDate, tradeDate)
						.orderByDesc(StockTopListEntity::getTradeDate));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		JSONArray data = new JSONArray();
		for (StockTopListEntity row : rows) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("trade_date", row.getTradeDate());
			item.put("pct_change", row.getPctChange());
			item.put("net_amount_yuan", row.getNetAmount());
			item.put("buy_amount_yuan", row.getLBuy());
			item.put("sell_amount_yuan", row.getLSell());
			item.put("turnover_rate", row.getTurnoverRate());
			item.put("reason", row.getReason());
			data.add(item);
		}
		resp.put("data", data);
		return meta(resp, tradeDate, !rows.isEmpty());
	}

	/**
	 * 主力资金流（Dify 资金流分析师数据源）
	 */
	@Operation(summary = "主力资金流", description = "近N个交易日主力/超大单净流入")
	@GetMapping("/money-flow")
	public String moneyFlow(@RequestParam("tsCode") String tsCode,
			@RequestParam(value = "days", required = false, defaultValue = "10") int days) {
		checkToken();
		List<StockMoneyFlowEntity> rows = stockMoneyFlowMapper.selectList(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.eq(StockMoneyFlowEntity::getTsCode, tsCode)
						.isNotNull(StockMoneyFlowEntity::getMainNetInflow)
						.orderByDesc(StockMoneyFlowEntity::getTradeDate)
						.last("limit " + Math.max(1, Math.min(days, 30))));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		JSONArray data = new JSONArray();
		for (StockMoneyFlowEntity row : rows) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("trade_date", row.getTradeDate());
			item.put("main_net_inflow_yuan", row.getMainNetInflow());
			item.put("main_net_pct", row.getMainNetPct());
			item.put("super_large_net_yuan", row.getSuperLargeNet());
			item.put("large_net_yuan", row.getLargeNet());
			item.put("industry", row.getIndustryName());
			data.add(item);
		}
		resp.put("data", data);
		String latest = rows.isEmpty() ? null : rows.get(0).getTradeDate();
		return meta(resp, latest, !rows.isEmpty());
	}

	/**
	 * 公告与新闻（Dify 新闻/政策分析师数据源）
	 */
	@Operation(summary = "公告与新闻", description = "近N天公告/媒体新闻标题摘要")
	@GetMapping("/news")
	public String news(@RequestParam("tsCode") String tsCode,
			@RequestParam(value = "days", required = false, defaultValue = "7") int days,
			@RequestParam(value = "type", required = false, defaultValue = "all") String type) {
		checkToken();
		String endDate = latestTradeDate();
		String start = minusDays(endDate, Math.max(days, 1));
		var wrapper = Wrappers.<StockMotAnnNewsEntity>lambdaQuery()
				.eq(StockMotAnnNewsEntity::getTsCode, tsCode)
				.ge(StockMotAnnNewsEntity::getPubDate, start)
				.le(StockMotAnnNewsEntity::getPubDate, endDate)
				.orderByDesc(StockMotAnnNewsEntity::getPubDate)
				.last("limit 50");
		if (!"all".equalsIgnoreCase(type)) {
			wrapper.eq(StockMotAnnNewsEntity::getNewsType, type);
		}
		List<StockMotAnnNewsEntity> rows = stockMotAnnNewsMapper.selectList(wrapper);
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		JSONArray data = new JSONArray();
		for (StockMotAnnNewsEntity row : rows) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("pub_date", row.getPubDate());
			item.put("news_type", row.getNewsType());
			item.put("src", row.getSrc());
			item.put("title", row.getTitle());
			item.put("summary", row.getSummary());
			data.add(item);
		}
		resp.put("data", data);
		return meta(resp, endDate, !rows.isEmpty());
	}

	/**
	 * 行业板块行情（Dify 板块分析师数据源）
	 */
	@Operation(summary = "行业板块行情", description = "所属行业近10个交易日涨跌幅")
	@GetMapping("/sector")
	public String sector(@RequestParam("tsCode") String tsCode) {
		checkToken();
		StockMoneyFlowEntity latestFlow = stockMoneyFlowMapper.selectOne(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.eq(StockMoneyFlowEntity::getTsCode, tsCode)
						.isNotNull(StockMoneyFlowEntity::getIndustryName)
						.orderByDesc(StockMoneyFlowEntity::getTradeDate)
						.last("limit 1"));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		if (latestFlow == null || StrUtil.isBlank(latestFlow.getIndustryName())) {
			resp.put("industry", null);
			return meta(resp, null, false);
		}
		resp.put("industry", latestFlow.getIndustryName());
		List<StockIndustryDailyEntity> rows = stockIndustryDailyMapper.selectList(
				Wrappers.<StockIndustryDailyEntity>lambdaQuery()
						.eq(StockIndustryDailyEntity::getBoardName, latestFlow.getIndustryName())
						.isNotNull(StockIndustryDailyEntity::getClose)
						.orderByDesc(StockIndustryDailyEntity::getTradeDate)
						.last("limit 11"));
		JSONArray data = new JSONArray();
		for (int i = 0; i < rows.size(); i++) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("trade_date", rows.get(i).getTradeDate());
			item.put("close", rows.get(i).getClose());
			if (i + 1 < rows.size() && rows.get(i + 1).getClose().doubleValue() > 0) {
				double pct = rows.get(i).getClose().doubleValue() / rows.get(i + 1).getClose().doubleValue() - 1;
				item.put("pct_chg", Math.round(pct * 10000) / 10000.0);
			}
			data.add(item);
		}
		resp.put("data", data);
		return meta(resp, rows.isEmpty() ? null : rows.get(0).getTradeDate(), rows.size() >= 2);
	}

	/**
	 * 股东增减持与股东户数（Dify 附加数据源）
	 */
	@Operation(summary = "股东增减持与户数", description = "高管增减持记录 + 股东户数变化")
	@GetMapping("/holder")
	public String holder(@RequestParam("tsCode") String tsCode) {
		checkToken();
		String endDate = latestTradeDate();
		String start = minusDays(endDate, 90);
		List<StockMotHolderEntity> holders = stockMotHolderMapper.selectList(
				Wrappers.<StockMotHolderEntity>lambdaQuery()
						.eq(StockMotHolderEntity::getTsCode, tsCode)
						.ge(StockMotHolderEntity::getAnnDate, start)
						.le(StockMotHolderEntity::getAnnDate, endDate)
						.orderByDesc(StockMotHolderEntity::getAnnDate)
						.last("limit 20"));
		List<StockMotHolderCountEntity> counts = stockMotHolderCountMapper.selectList(
				Wrappers.<StockMotHolderCountEntity>lambdaQuery()
						.eq(StockMotHolderCountEntity::getTsCode, tsCode)
						.orderByDesc(StockMotHolderCountEntity::getEndDate)
						.last("limit 4"));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		JSONArray holderArr = new JSONArray();
		for (StockMotHolderEntity row : holders) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("ann_date", row.getAnnDate());
			item.put("holder_name", row.getHolderName());
			item.put("holder_type", row.getHolderType());
			item.put("direction", "IN".equals(row.getInDes()) ? "增持" : "减持");
			item.put("change_vol", row.getChangeVol());
			item.put("change_ratio_pct", row.getChangeRatio());
			holderArr.add(item);
		}
		resp.put("holder_changes", holderArr);
		JSONArray countArr = new JSONArray();
		for (StockMotHolderCountEntity row : counts) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("ann_date", row.getAnnDate());
			item.put("end_date", row.getEndDate());
			item.put("holder_num", row.getHolderNum());
			countArr.add(item);
		}
		resp.put("holder_counts", countArr);
		return meta(resp, endDate, !holders.isEmpty() || !counts.isEmpty());
	}

	/**
	 * 大盘环境（沪深300 近期收益）
	 */
	@Operation(summary = "大盘环境", description = "沪深300近5/20日收益")
	@GetMapping("/market-env")
	public String marketEnv(@RequestParam(value = "date", required = false) String date) {
		checkToken();
		String tradeDate = resolveDate(date);
		List<StockIndexDailyEntity> rows = stockIndexDailyMapper.selectList(
				Wrappers.<StockIndexDailyEntity>lambdaQuery()
						.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
						.le(StockIndexDailyEntity::getTradeDate, tradeDate)
						.orderByDesc(StockIndexDailyEntity::getTradeDate)
						.last("limit 21"));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("index", "CSI300");
		resp.put("trade_date", tradeDate);
		if (rows.size() >= 2) {
			double latest = rows.get(0).getClose().doubleValue();
			if (rows.size() >= 6) {
				double d5 = rows.get(5).getClose().doubleValue();
				resp.put("ret_5d", Math.round((latest / d5 - 1) * 10000) / 10000.0);
			}
			if (rows.size() >= 21) {
				double d20 = rows.get(20).getClose().doubleValue();
				resp.put("ret_20d", Math.round((latest / d20 - 1) * 10000) / 10000.0);
			}
			resp.put("latest_close", latest);
		}
		return meta(resp, tradeDate, rows.size() >= 6);
	}

	/**
	 * 日K线（LLM 需要原始价格序列时的补充数据源）
	 */
	@Operation(summary = "日K线", description = "近N根日线OHLCV")
	@GetMapping("/kline")
	public String kline(@RequestParam("tsCode") String tsCode,
			@RequestParam(value = "limit", required = false, defaultValue = "30") int limit) {
		checkToken();
		String tradeDate = latestTradeDate();
		List<StockDailyEntity> rows = stockDailyMapper.selectList(
				Wrappers.<StockDailyEntity>lambdaQuery()
						.eq(StockDailyEntity::getTsCode, tsCode)
						.le(StockDailyEntity::getTradeDate, tradeDate)
						.orderByDesc(StockDailyEntity::getTradeDate)
						.last("limit " + Math.max(1, Math.min(limit, 120))));
		JSONObject resp = new JSONObject(new LinkedHashMap<>());
		resp.put("ts_code", tsCode);
		JSONArray data = new JSONArray();
		for (StockDailyEntity row : rows) {
			JSONObject item = new JSONObject(new LinkedHashMap<>());
			item.put("trade_date", row.getTradeDate());
			item.put("open", row.getOpen());
			item.put("high", row.getHigh());
			item.put("low", row.getLow());
			item.put("close", row.getClose());
			item.put("pct_chg", row.getPctChg());
			item.put("amount_yuan", row.getAmount() != null ? row.getAmount().doubleValue() * 1000 : null);
			data.add(item);
		}
		resp.put("data", data);
		return meta(resp, tradeDate, !rows.isEmpty());
	}

	/**
	 * OpenAPI 3 规范（直接导入 Dify 自定义工具）
	 */
	@Operation(summary = "OpenAPI规范", description = "输出 OpenAPI 3 JSON，供 Dify 自定义工具导入")
	@GetMapping(value = "/openapi.json", produces = "application/json")
	public String openapi() {
		JSONObject spec = JSON.parseObject("""
				{
				  "openapi": "3.0.0",
				  "info": {"title": "NQBoard Quanta Data API", "version": "1.0.0",
				    "description": "A股短线分析数据供给接口（供 Dify Workflow HTTP 节点调用）"},
				  "servers": [{"url": "http://your-host:9999/quanta"}],
				  "paths": {
				    "/dify/technicals": {"get": {"summary": "技术面特征与筛选打分",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}, "description": "TS股票代码，如 600519.SH"},
				        {"name": "date", "in": "query", "required": false, "schema": {"type": "string"}, "description": "基准日 YYYYMMDD，默认最新交易日"}],
				      "responses": {"200": {"description": "特征向量+打分"}}}},
				    "/dify/dragon-tiger": {"get": {"summary": "龙虎榜记录",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}},
				        {"name": "days", "in": "query", "required": false, "schema": {"type": "integer", "default": 5}, "description": "回看交易日数"}],
				      "responses": {"200": {"description": "上榜记录与净买额"}}}},
				    "/dify/money-flow": {"get": {"summary": "主力资金流",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}},
				        {"name": "days", "in": "query", "required": false, "schema": {"type": "integer", "default": 10}, "description": "回看交易日数"}],
				      "responses": {"200": {"description": "主力/超大单净流入"}}}},
				    "/dify/news": {"get": {"summary": "公告与新闻",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}},
				        {"name": "days", "in": "query", "required": false, "schema": {"type": "integer", "default": 7}},
				        {"name": "type", "in": "query", "required": false, "schema": {"type": "string", "enum": ["all", "ann", "media"], "default": "all"}}],
				      "responses": {"200": {"description": "公告/新闻标题摘要"}}}},
				    "/dify/sector": {"get": {"summary": "行业板块行情",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}}],
				      "responses": {"200": {"description": "所属行业近10日涨跌幅"}}}},
				    "/dify/holder": {"get": {"summary": "股东增减持与户数",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}}],
				      "responses": {"200": {"description": "增减持记录+股东户数"}}}},
				    "/dify/market-env": {"get": {"summary": "大盘环境",
				      "parameters": [
				        {"name": "date", "in": "query", "required": false, "schema": {"type": "string"}}],
				      "responses": {"200": {"description": "沪深300近5/20日收益"}}}},
				    "/dify/kline": {"get": {"summary": "日K线",
				      "parameters": [
				        {"name": "tsCode", "in": "query", "required": true, "schema": {"type": "string"}},
				        {"name": "limit", "in": "query", "required": false, "schema": {"type": "integer", "default": 30}}],
				      "responses": {"200": {"description": "近N根日线OHLCV"}}}}
				  }
				}
				""");
		return spec.toJSONString();
	}

	// ==================== 内部方法 ====================

	private void checkToken() {
		if (StrUtil.isBlank(toolsToken)) {
			return; // 未配置时放行（本地调试）
		}
		var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
		if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttrs) {
			String token = servletAttrs.getRequest().getHeader("X-Dify-Token");
			if (!toolsToken.equals(token)) {
				throw new org.springframework.web.server.ResponseStatusException(
						org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid X-Dify-Token");
			}
		}
	}

	private String resolveDate(String date) {
		return StrUtil.isNotBlank(date) ? date : latestTradeDate();
	}

	private String latestTradeDate() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(
				Wrappers.<StockIndexDailyEntity>lambdaQuery()
						.select(StockIndexDailyEntity::getTradeDate)
						.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
						.orderByDesc(StockIndexDailyEntity::getTradeDate)
						.last("limit 1"));
		return latest != null ? latest.getTradeDate() : LocalDate.now().format(BASIC_DATE);
	}

	private String minusDays(String basicDate, int days) {
		LocalDate d = LocalDate.parse(basicDate.length() == 8
				? basicDate.substring(0, 4) + "-" + basicDate.substring(4, 6) + "-" + basicDate.substring(6, 8)
				: basicDate);
		return d.minusDays(days).format(BASIC_DATE);
	}

	/**
	 * 附加 _meta 并输出裸 JSON
	 */
	private String meta(JSONObject resp, String dataDate, boolean complete) {
		JSONObject meta = new JSONObject(new LinkedHashMap<>());
		meta.put("data_date", dataDate);
		meta.put("complete", complete);
		meta.put("note", complete ? null : "数据缺失，请降低置信度或输出 n/a");
		resp.put("_meta", meta);
		return resp.toJSONString();
	}

}
