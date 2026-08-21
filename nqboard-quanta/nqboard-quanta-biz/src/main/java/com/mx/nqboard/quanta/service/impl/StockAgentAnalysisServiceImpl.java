package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockAgentAnalysisEntity;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.dify.AgentFallbackService;
import com.mx.nqboard.quanta.dify.DifyAnalysisResult;
import com.mx.nqboard.quanta.dify.DifyClient;
import com.mx.nqboard.quanta.mapper.StockAgentAnalysisMapper;
import com.mx.nqboard.quanta.service.StockAgentAnalysisService;
import com.mx.nqboard.quanta.service.StockCandidateService;
import com.mx.nqboard.quanta.service.StockScreenResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * LLM逐Agent分析结果 服务实现类（Dify 编排 + 加权聚合 Gate）
 * </p>
 * <p>
 * 聚合策略（LLM 做减法比做加法可靠）：
 * <pre>
 * 1. 加权分：sum(weight * signal(±1/0) * conf/100) / sum(有效weight)，n/a 跳过
 * 2. 反向一票否决：任一 Agent bearish 且置信度>=70 且加权分<0 -> action 降为 avoid
 * 3. 同向提升：加权分>0 -> action 保持 entry_ok，confidence = 规则置信度与 LLM 置信度取高
 * 4. Dify 整体失败 -> 全部规则降级（decision_mode=rules_fallback），流水线不中断
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAgentAnalysisServiceImpl extends ServiceImpl<StockAgentAnalysisMapper, StockAgentAnalysisEntity>
		implements StockAgentAnalysisService {

	/**
	 * Agent 权重（移植自 ai-hedge-fund SHORT_TERM_ANALYST_WEIGHTS，trend_predictor 未纳入 Dify）
	 */
	private static final Map<String, Double> AGENT_WEIGHTS = Map.of(
			"technical", 0.30,
			"sector", 0.15,
			"money_flow", 0.15,
			"dragon_tiger", 0.10,
			"news", 0.10,
			"policy", 0.10);

	/**
	 * 反向一票否决的置信度门槛
	 */
	private static final int VETO_CONFIDENCE = 70;

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockAgentAnalysisMapper stockAgentAnalysisMapper;

	private final StockCandidateService stockCandidateService;

	private final StockScreenResultService stockScreenResultService;

	private final DifyClient difyClient;

	private final AgentFallbackService agentFallbackService;

	/**
	 * 每日 LLM 调用上限（成本护栏）
	 */
	@Value("${dify.daily-max-calls:10}")
	private int dailyMaxCalls;

	/**
	 * 相邻候选调用间隔（毫秒）：控制模型侧请求频率，避免触发 429 限流
	 */
	@Value("${dify.call-interval-ms:10000}")
	private long callIntervalMs;

	@Override
	public int analyze() {
		StockCandidateEntity latest = stockCandidateService.getOne(
				Wrappers.<StockCandidateEntity>lambdaQuery()
						.select(StockCandidateEntity::getTradeDate)
						.orderByDesc(StockCandidateEntity::getTradeDate)
						.last("limit 1"));
		if (latest == null) {
			log.info("LLM分析跳过：候选池为空");
			return 0;
		}
		return analyze(latest.getTradeDate());
	}

	@Override
	public int analyze(String tradeDate) {
		// 当日候选（entry_ok 且 ACTIVE）
		List<StockCandidateEntity> candidates = stockCandidateService.list(
				Wrappers.<StockCandidateEntity>lambdaQuery()
						.eq(StockCandidateEntity::getTradeDate, tradeDate)
						.eq(StockCandidateEntity::getAction, "entry_ok")
						.eq(StockCandidateEntity::getStatus, "ACTIVE")
						.orderByDesc(StockCandidateEntity::getScreenScore));
		if (CollUtil.isEmpty(candidates)) {
			log.info("LLM分析跳过：信号日 {} 无 entry_ok 候选", tradeDate);
			return 0;
		}
		int limit = Math.min(candidates.size(), dailyMaxCalls);
		boolean difyAvailable = difyClient.available();
		if (!difyAvailable) {
			log.warn("Dify 不可用（未配置或未启用），全部走规则降级模式");
		}

		int processed = 0;
		for (int i = 0; i < limit; i++) {
			StockCandidateEntity candidate = candidates.get(i);
			try {
				DifyAnalysisResult result;
				String decisionMode;
				String modelName = null;
				if (difyAvailable) {
					try {
						StockScreenResultEntity screen = stockScreenResultService.getOne(
								Wrappers.<StockScreenResultEntity>lambdaQuery()
										.eq(StockScreenResultEntity::getTradeDate, tradeDate)
										.eq(StockScreenResultEntity::getTsCode, candidate.getTsCode())
										.last("limit 1"));
						Double score = screen != null && screen.getScreenScore() != null
								? screen.getScreenScore().doubleValue() : null;
						result = difyClient.analyze(candidate.getTsCode(), tradeDate, score,
								screen != null ? screen.getMetrics() : null);
						decisionMode = "agent";
					}
					catch (Exception e) {
						log.warn("Dify 分析 {} 失败，规则降级: {}", candidate.getTsCode(), e.getMessage());
						result = fallbackResult(candidate.getTsCode(), tradeDate);
						decisionMode = "rules_fallback";
					}
				}
				else {
					result = fallbackResult(candidate.getTsCode(), tradeDate);
					decisionMode = "rules_fallback";
				}

				// 逐 Agent 落库
				saveAgents(tradeDate, candidate.getTsCode(), result, decisionMode);

				// 聚合 + 回写候选
				applyToCandidate(candidate, result, decisionMode);
				processed++;
			}
			catch (Exception e) {
				log.error("LLM 分析 {} 异常（跳过，不影响其他候选）", candidate.getTsCode(), e);
			}
			// 相邻候选间隔，控制模型侧请求频率（最后一个不等待）
			if (i < limit - 1 && callIntervalMs > 0 && difyAvailable) {
				try {
					Thread.sleep(callIntervalMs);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		log.info("LLM分析完成: 基准日={}, 候选={}, 处理={}, 模式={}", tradeDate, candidates.size(), processed,
				difyAvailable ? "agent" : "rules_fallback");
		return processed;
	}

	/**
	 * 构造规则降级结果
	 */
	private DifyAnalysisResult fallbackResult(String tsCode, String tradeDate) {
		DifyAnalysisResult result = new DifyAnalysisResult();
		List<DifyAnalysisResult.AgentResult> agents = agentFallbackService.fallbackAgents(tsCode, tradeDate, true);
		result.setAgents(agents);
		result.setReasons(List.of("规则降级模式"));
		result.setAction("watch");
		result.setConfidence(55);
		return result;
	}

	/**
	 * 逐 Agent 结果落库（按 trade_date + ts_code + agent_key upsert）
	 */
	private void saveAgents(String tradeDate, String tsCode, DifyAnalysisResult result, String decisionMode) {
		if (CollUtil.isEmpty(result.getAgents())) {
			return;
		}
		Map<String, StockAgentAnalysisEntity> existMap = list(Wrappers.<StockAgentAnalysisEntity>lambdaQuery()
				.eq(StockAgentAnalysisEntity::getTradeDate, tradeDate)
				.eq(StockAgentAnalysisEntity::getTsCode, tsCode))
				.stream()
				.collect(java.util.stream.Collectors.toMap(StockAgentAnalysisEntity::getAgentKey, e -> e));
		List<StockAgentAnalysisEntity> toSave = new ArrayList<>();
		for (DifyAnalysisResult.AgentResult agent : result.getAgents()) {
			StockAgentAnalysisEntity entity = new StockAgentAnalysisEntity();
			entity.setTradeDate(tradeDate);
			entity.setTsCode(tsCode);
			entity.setAgentKey(agent.getKey());
			entity.setSignal(StrUtil.nullToEmpty(agent.getSignal()).toLowerCase());
			entity.setConfidence(agent.getConfidence());
			entity.setReasoning(StrUtil.maxLength(agent.getReasoning(), 1000));
			entity.setDecisionMode(decisionMode);
			if (result.getElapsedSeconds() != null) {
				entity.setLatencyMs((int) (result.getElapsedSeconds() * 1000));
			}
			StockAgentAnalysisEntity exist = existMap.get(agent.getKey());
			if (exist != null) {
				entity.setId(exist.getId());
			}
			toSave.add(entity);
		}
		saveOrUpdateBatch(toSave, 100);
		// 原始输出仅存首行（避免重复大字段）
		if (StrUtil.isNotBlank(result.getRawOutput())) {
			StockAgentAnalysisEntity first = toSave.get(0);
			first.setRawOutput(StrUtil.maxLength(result.getRawOutput(), 5000));
			updateById(first);
		}
	}

	/**
	 * 加权聚合 + 合并 Gate，回写候选池
	 */
	private void applyToCandidate(StockCandidateEntity candidate, DifyAnalysisResult result, String decisionMode) {
		// 加权分（n/a 跳过，权重归一化）
		double weightedScore = 0;
		double totalWeight = 0;
		boolean strongBearish = false;
		int bullishCount = 0;
		for (DifyAnalysisResult.AgentResult agent : result.getAgents()) {
			Double weight = AGENT_WEIGHTS.get(agent.getKey());
			if (weight == null) {
				continue;
			}
			String signal = StrUtil.nullToEmpty(agent.getSignal()).toLowerCase();
			if ("n/a".equals(signal) || StrUtil.isBlank(signal)) {
				continue;
			}
			int conf = agent.getConfidence() != null ? Math.max(0, Math.min(100, agent.getConfidence())) : 50;
			double sigVal = "bullish".equals(signal) ? 1 : ("bearish".equals(signal) ? -1 : 0);
			weightedScore += weight * sigVal * conf / 100.0;
			totalWeight += weight;
			if ("bullish".equals(signal)) {
				bullishCount++;
			}
			if ("bearish".equals(signal) && conf >= VETO_CONFIDENCE) {
				strongBearish = true;
			}
		}
		if (totalWeight > 0) {
			weightedScore /= totalWeight;
		}

		// 合并 Gate：反向一票否决 / 同向保持 / 中性观察
		String action = candidate.getAction();
		List<String> reasons = parseReasons(candidate.getReasons());
		int confidence = candidate.getConfidence() != null ? candidate.getConfidence() : 55;
		if (strongBearish && weightedScore < 0) {
			action = "avoid";
			reasons.add("LLM反向一票否决");
		}
		else if (weightedScore > 0 && "entry_ok".equals(action)) {
			// 同向：LLM 置信度取高（LLM 只减分不加分的主逻辑在否决分支）
			if (result.getConfidence() != null && result.getConfidence() > confidence) {
				confidence = Math.min(95, result.getConfidence());
			}
		}
		else if (weightedScore <= 0 && "entry_ok".equals(action) && bullishCount == 0) {
			action = "watch";
			reasons.add("LLM偏空观望");
		}

		candidate.setLlmScore(BigDecimal.valueOf(Math.round(weightedScore * 10000) / 10000.0));
		candidate.setAction(action);
		candidate.setConfidence(confidence);
		candidate.setDecisionMode(decisionMode);
		candidate.setReasons(JSON.toJSONString(reasons));
		// agent_summary: [{key,signal,confidence,reasoning}]
		List<Map<String, Object>> summary = new ArrayList<>();
		for (DifyAnalysisResult.AgentResult agent : result.getAgents()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("key", agent.getKey());
			item.put("signal", agent.getSignal());
			item.put("confidence", agent.getConfidence());
			item.put("reasoning", agent.getReasoning());
			summary.add(item);
		}
		candidate.setAgentSummary(JSON.toJSONString(summary));
		stockCandidateService.updateById(candidate);
		log.info("候选 {} LLM聚合: mode={}, llmScore={}, action={}, bullish={}", candidate.getTsCode(), decisionMode,
				weightedScore, action, bullishCount);
	}

	private List<String> parseReasons(String reasonsJson) {
		if (StrUtil.isBlank(reasonsJson)) {
			return new ArrayList<>();
		}
		try {
			return new ArrayList<>(JSON.parseArray(reasonsJson, String.class));
		}
		catch (Exception e) {
			return new ArrayList<>();
		}
	}

}
