package com.mx.nqboard.quanta.dify;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Dify Workflow API 客户端
 * </p>
 * <p>
 * 调用约定：Java 推（每只候选股一次 workflow run，inputs 携带 ts_code/trade_date/规则打分），
 * Dify 内 HTTP 节点固定取数（回调 nqboard /dify/* 接口）、LLM 节点逐分析师判断、
 * Code 节点聚合、End 节点输出结构化 JSON。Java 只做调度与落库。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Component
public class DifyClient {

	/**
	 * Dify Workflow 运行接口
	 */
	private static final String WORKFLOW_RUN_PATH = "/workflows/run";

	/**
	 * Dify 有效状态
	 */
	private static final String STATUS_SUCCEEDED = "succeeded";

	@org.springframework.beans.factory.annotation.Value("${dify.base-url:}")
	private String baseUrl;

	@org.springframework.beans.factory.annotation.Value("${dify.workflow-key:}")
	private String workflowKey;

	/**
	 * 请求超时（毫秒）
	 */
	@Value("${dify.timeout-ms:60000}")
	private int timeoutMs;

	/**
	 * 失败重试次数
	 */
	@Value("${dify.retry:2}")
	private int retry;

	/**
	 * 重试间隔（毫秒）：模型侧 429 限流多为并发/频率限制，退避后重试
	 */
	@Value("${dify.retry-backoff-ms:20000}")
	private long retryBackoffMs;

	/**
	 * 是否启用（未配置 key 时自动 false）
	 */
	@Value("${dify.enabled:true}")
	private boolean enabled;

	/**
	 * Dify 是否可用（配置齐全且启用）
	 */
	public boolean available() {
		return enabled && StrUtil.isNotBlank(baseUrl) && StrUtil.isNotBlank(workflowKey);
	}

	/**
	 * 调用 Workflow 分析单只股票
	 * @param tsCode TS股票代码
	 * @param tradeDate 分析基准日 YYYYMMDD
	 * @param screenScore 规则打分（供 LLM 参考）
	 * @param metricsJson 筛选特征向量 JSON（供 LLM 参考）
	 * @return 解析后的分析结果
	 */
	public DifyAnalysisResult analyze(String tsCode, String tradeDate, Double screenScore, String metricsJson) {
		if (!available()) {
			throw new IllegalStateException("Dify 未配置（dify.base-url / dify.workflow-key）或未启用");
		}
		Map<String, Object> inputs = new LinkedHashMap<>();
		inputs.put("ts_code", tsCode);
		inputs.put("trade_date", tradeDate);
		inputs.put("screen_score", screenScore);
		inputs.put("metrics", StrUtil.nullToEmpty(metricsJson));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("inputs", inputs);
		body.put("response_mode", "blocking");
		body.put("user", "quanta-screener");

		Exception last = null;
		for (int attempt = 1; attempt <= retry + 1; attempt++) {
			try {
				return doRun(body);
			}
			catch (Exception e) {
				last = e;
				log.warn("Dify workflow 调用失败(第 {}/{} 次) {}: {}", attempt, retry + 1, tsCode, e.getMessage());
				// 退避后重试（最后一次失败无需等待）
				if (attempt <= retry && retryBackoffMs > 0) {
					try {
						Thread.sleep(retryBackoffMs);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		throw new IllegalStateException("Dify workflow 调用失败: " + (last != null ? last.getMessage() : "unknown"), last);
	}

	private DifyAnalysisResult doRun(Map<String, Object> body) {
		String respBody = HttpRequest.post(baseUrl + WORKFLOW_RUN_PATH)
				.header("Authorization", "Bearer " + workflowKey)
				.header("Content-Type", "application/json")
				.body(JSON.toJSONString(body))
				.timeout(timeoutMs)
				.execute()
				.body();
		JSONObject resp = JSON.parseObject(respBody);
		if (resp == null) {
			throw new IllegalStateException("Dify 响应为空");
		}
		JSONObject data = resp.getJSONObject("data");
		if (data == null) {
			throw new IllegalStateException("Dify 响应缺少 data: " + StrUtil.maxLength(respBody, 300));
		}
		String status = data.getString("status");
		if (!STATUS_SUCCEEDED.equals(status)) {
			throw new IllegalStateException("Dify workflow 状态异常: " + status + ", error="
					+ StrUtil.maxLength(StrUtil.nullToEmpty(data.getString("error")), 300));
		}
		JSONObject outputs = data.getJSONObject("outputs");
		if (outputs == null) {
			throw new IllegalStateException("Dify workflow 无 outputs");
		}
		return parseOutputs(outputs, respBody);
	}

	/**
	 * 解析 outputs（兼容两种形态：对象直出 / result 字符串内嵌 JSON）
	 */
	private DifyAnalysisResult parseOutputs(JSONObject outputs, String rawBody) {
		JSONObject source = outputs;
		Object resultRaw = outputs.get("result");
		String resultStr = null;
		JSONObject resultObj = null;
		if (resultRaw instanceof String) {
			resultStr = (String) resultRaw;
		}
		else if (resultRaw instanceof JSONObject) {
			resultObj = (JSONObject) resultRaw;
		}
		if (resultObj != null) {
			source = resultObj;
		}
		else if (StrUtil.isNotBlank(resultStr)) {
			try {
				source = JSON.parseObject(resultStr);
			}
			catch (Exception e) {
				throw new IllegalStateException("Dify outputs.result 非合法 JSON: "
						+ StrUtil.maxLength(resultStr, 300));
			}
		}
		DifyAnalysisResult result = new DifyAnalysisResult();
		result.setAction(source.getString("action"));
		result.setConfidence(source.getInteger("confidence"));
		result.setWeightedScore(source.getDouble("weighted_score"));
		result.setReasons(toStringList(source.getJSONArray("reasons")));
		result.setRiskFlags(toStringList(source.getJSONArray("risk_flags")));

		List<DifyAnalysisResult.AgentResult> agents = new ArrayList<>();
		Object agentsRaw = source.get("agents");
		JSONArray agentArr = null;
		if (agentsRaw instanceof JSONArray) {
			agentArr = (JSONArray) agentsRaw;
		}
		else if (agentsRaw instanceof String) {
			try {
				agentArr = JSON.parseArray((String) agentsRaw);
			}
			catch (Exception ignore) {
				agentArr = null;
			}
		}
		if (agentArr != null) {
			for (int i = 0; i < agentArr.size(); i++) {
				Object item = agentArr.get(i);
				JSONObject a;
				if (item instanceof String) {
					try {
						a = JSON.parseObject((String) item);
					}
					catch (Exception e) {
						continue;
					}
				}
				else if (item instanceof JSONObject) {
					a = (JSONObject) item;
				}
				else {
					continue;
				}
				DifyAnalysisResult.AgentResult agent = new DifyAnalysisResult.AgentResult();
				agent.setKey(a.getString("key"));
				agent.setSignal(StrUtil.nullToEmpty(a.getString("signal")).toLowerCase());
				agent.setConfidence(a.getInteger("confidence"));
				agent.setReasoning(a.getString("reasoning"));
				agents.add(agent);
			}
		}
		if (agents.isEmpty()) {
			// 兼容 Dify 直接输出 {technical, sector, money_flow, dragon_tiger, news:{agents:[...]}} 的形态
			addAgentIfPresent(source, agents, "technical");
			addAgentIfPresent(source, agents, "sector");
			addAgentIfPresent(source, agents, "money_flow");
			addAgentIfPresent(source, agents, "dragon_tiger");
			Object newsRaw = source.get("news");
			if (newsRaw != null) {
				JSONObject newsObj = null;
				if (newsRaw instanceof String) {
					try {
						newsObj = JSON.parseObject((String) newsRaw);
					}
					catch (Exception e) {
						newsObj = null;
					}
				}
				else if (newsRaw instanceof JSONObject) {
					newsObj = (JSONObject) newsRaw;
				}
				if (newsObj != null) {
					Object newsAgentsRaw = newsObj.get("agents");
					JSONArray newsAgents = null;
					if (newsAgentsRaw instanceof JSONArray) {
						newsAgents = (JSONArray) newsAgentsRaw;
					}
					else if (newsAgentsRaw instanceof String) {
						try {
							newsAgents = JSON.parseArray((String) newsAgentsRaw);
						}
						catch (Exception ignore) {
							newsAgents = null;
						}
					}
					if (newsAgents != null) {
						for (int i = 0; i < newsAgents.size(); i++) {
							Object newsItem = newsAgents.get(i);
							JSONObject a;
							if (newsItem instanceof String) {
								try {
									a = JSON.parseObject((String) newsItem);
								}
								catch (Exception e) {
									continue;
								}
							}
							else if (newsItem instanceof JSONObject) {
								a = (JSONObject) newsItem;
							}
							else {
								continue;
							}
							DifyAnalysisResult.AgentResult agent = new DifyAnalysisResult.AgentResult();
							agent.setKey(a.getString("key"));
							agent.setSignal(StrUtil.nullToEmpty(a.getString("signal")).toLowerCase());
							agent.setConfidence(a.getInteger("confidence"));
							agent.setReasoning(a.getString("reasoning"));
							agents.add(agent);
						}
					}
				}
			}
		}
		if (agents.isEmpty()) {
			log.warn("Dify outputs 解析后 agents 为空, resultStr={}, outputs={}, rawBody={}",
					StrUtil.maxLength(resultStr, 500),
					StrUtil.maxLength(outputs.toJSONString(), 2000),
					StrUtil.maxLength(rawBody, 2000));
		}
		result.setAgents(agents);
		JSONObject data = JSON.parseObject(rawBody).getJSONObject("data");
		if (data != null) {
			result.setTotalTokens(data.getInteger("total_tokens"));
			result.setElapsedSeconds(data.getDouble("elapsed_time"));
		}
		result.setRawOutput(rawBody);
		return result;
	}

	private void addAgentIfPresent(JSONObject source, List<DifyAnalysisResult.AgentResult> agents, String key) {
		Object raw = source.get(key);
		if (raw == null) {
			return;
		}
		JSONObject obj;
		if (raw instanceof String) {
			try {
				obj = JSON.parseObject((String) raw);
			}
			catch (Exception e) {
				return;
			}
		}
		else if (raw instanceof JSONObject) {
			obj = (JSONObject) raw;
		}
		else {
			return;
		}
		DifyAnalysisResult.AgentResult agent = new DifyAnalysisResult.AgentResult();
		agent.setKey(StrUtil.blankToDefault(obj.getString("key"), key));
		agent.setSignal(StrUtil.nullToEmpty(obj.getString("signal")).toLowerCase());
		agent.setConfidence(obj.getInteger("confidence"));
		agent.setReasoning(obj.getString("reasoning"));
		agents.add(agent);
	}


	private List<String> toStringList(JSONArray arr) {
		List<String> list = new ArrayList<>();
		if (arr != null) {
			for (int i = 0; i < arr.size(); i++) {
				list.add(arr.getString(i));
			}
		}
		return list;
	}

}
