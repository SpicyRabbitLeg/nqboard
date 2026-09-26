package com.mx.nqboard.export.dify;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mx.nqboard.common.core.exception.CheckedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <p>
 * Dify Workflow API 客户端（专家抽取通用 LLM 任务）
 * </p>
 * <p>
 * 调用约定：后端组装完整提示词，通过 Dify 工作流的 inputs 携带 task（任务类型）与 payload（任务数据），
 * 工作流 End 节点输出变量固定为 result（JSON 字符串）。配置项 dify.base-url 需包含 /v1 前缀。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Component
public class DifyClient {

	/**
	 * Dify Workflow 运行接口
	 */
	private static final String WORKFLOW_RUN_PATH = "/workflows/run";

	/**
	 * Dify 成功状态
	 */
	private static final String STATUS_SUCCEEDED = "succeeded";

	@Value("${dify.base-url:}")
	private String baseUrl;

	@Value("${dify.workflow-key:}")
	private String workflowKey;

	/**
	 * 知识库 API Key（dataset- 开头，Dify 控制台-知识库-API 获取）
	 */
	@Value("${dify.knowledge-api-key:}")
	private String knowledgeApiKey;

	/**
	 * 请求超时（毫秒）
	 */
	@Value("${dify.timeout-ms:120000}")
	private int timeoutMs;

	/**
	 * 失败重试次数
	 */
	@Value("${dify.retry:2}")
	private int retry;

	/**
	 * 执行 LLM 任务
	 * @param task 任务类型（tag 打标 / propose 领域提案 / merge 领域合并 / parse 意图解析）
	 * @param payload 任务数据（完整提示词或 JSON 数据）
	 * @return 工作流 End 节点 result 变量内容（已去除 markdown 围栏）
	 */
	public String runLLMTask(String task, String payload) {
		if (StrUtil.isBlank(baseUrl) || StrUtil.isBlank(workflowKey)) {
			throw new CheckedException("Dify 未配置，请检查 dify.base-url / dify.workflow-key 配置项");
		}
		Map<String, Object> body = Map.of("inputs", Map.of("task", task, "payload", payload), "response_mode",
				"blocking", "user", "nqboard");
		int attempts = 0;
		Exception lastError = null;
		while (attempts <= retry) {
			try {
				String resp = HttpRequest.post(baseUrl + WORKFLOW_RUN_PATH)
					.header("Authorization", "Bearer " + workflowKey)
					.header("Content-Type", "application/json")
					.body(JSONUtil.toJsonStr(body))
					.timeout(timeoutMs)
					.execute()
					.body();
				JSONObject json = JSONUtil.parseObj(resp);
				JSONObject data = json.getJSONObject("data");
				if (data == null || !STATUS_SUCCEEDED.equals(data.getStr("status"))) {
					throw new CheckedException("Dify 工作流执行失败: " + StrUtil.subPre(resp, 500));
				}
				String result = data.getJSONObject("outputs").getStr("result");
				if (StrUtil.isBlank(result)) {
					throw new CheckedException("Dify 工作流返回 result 为空");
				}
				return cleanLLMOutput(result);
			}
			catch (Exception e) {
				lastError = e;
				attempts++;
				log.warn("Dify 任务[{}]第{}次调用失败: {}", task, attempts, e.getMessage());
			}
		}
		throw new CheckedException("Dify 任务[" + task + "]调用失败（已重试 " + retry + " 次）", lastError);
	}

	/**
	 * 知识库 API 是否已配置
	 */
	public boolean isKnowledgeConfigured() {
		return StrUtil.isNotBlank(baseUrl) && StrUtil.isNotBlank(knowledgeApiKey);
	}

	/**
	 * 查询数据集列表
	 */
	public JSONArray listDatasets() {
		String resp = httpGet("/datasets?page=1&limit=100");
		return JSONUtil.parseObj(resp).getJSONArray("data");
	}

	/**
	 * 按名称查找数据集 id，不存在返回 null
	 */
	public String findDatasetId(String name) {
		JSONArray datasets = listDatasets();
		if (datasets == null) {
			return null;
		}
		for (Object item : datasets) {
			JSONObject ds = (JSONObject) item;
			if (name.equals(ds.getStr("name"))) {
				return ds.getStr("id");
			}
		}
		return null;
	}

	/**
	 * 创建数据集
	 */
	public String createDataset(String name) {
		String body = JSONUtil
			.toJsonStr(Map.of("name", name, "indexing_technique", "high_quality", "permission", "all_team_members"));
		JSONObject json = JSONUtil.parseObj(httpPost("/datasets", body));
		return json.getStr("id");
	}

	/**
	 * 删除数据集
	 */
	public void deleteDataset(String datasetId) {
		httpDelete("/datasets/" + datasetId);
	}

	/**
	 * 查询数据集文档列表
	 */
	public JSONArray listDocuments(String datasetId, int page, int limit) {
		String resp = httpGet("/datasets/" + datasetId + "/documents?page=" + page + "&limit=" + limit);
		return JSONUtil.parseObj(resp).getJSONArray("data");
	}

	/**
	 * 删除数据集文档
	 */
	public void deleteDocument(String datasetId, String documentId) {
		httpDelete("/datasets/" + datasetId + "/documents/" + documentId);
	}

	/**
	 * 以文本文件创建文档（一文件多段落，段落分隔符由 process_rule 指定）
	 * @param dataJson create-by-file 的 data 表单参数（indexing_technique/process_rule 等）
	 */
	public JSONObject createDocumentByFile(String datasetId, String fileName, byte[] content, String dataJson) {
		cn.hutool.http.HttpResponse response = HttpRequest
			.post(knowledgeUrl("/datasets/" + datasetId + "/document/create-by-file"))
			.header("Authorization", "Bearer " + knowledgeApiKey)
			.form("file", content, fileName)
			.form("data", dataJson)
			.timeout(timeoutMs)
			.execute();
		return JSONUtil.parseObj(checkKnowledgeResponse(response.getStatus(), response.body()));
	}

	private String knowledgeUrl(String path) {
		return baseUrl + path;
	}

	private String httpGet(String path) {
		cn.hutool.http.HttpResponse response = HttpRequest.get(knowledgeUrl(path))
			.header("Authorization", "Bearer " + knowledgeApiKey)
			.timeout(timeoutMs)
			.execute();
		return checkKnowledgeResponse(response.getStatus(), response.body());
	}

	private String httpPost(String path, String body) {
		cn.hutool.http.HttpResponse response = HttpRequest.post(knowledgeUrl(path))
			.header("Authorization", "Bearer " + knowledgeApiKey)
			.header("Content-Type", "application/json")
			.body(body)
			.timeout(timeoutMs)
			.execute();
		return checkKnowledgeResponse(response.getStatus(), response.body());
	}

	private void httpDelete(String path) {
		cn.hutool.http.HttpResponse response = HttpRequest.delete(knowledgeUrl(path))
			.header("Authorization", "Bearer " + knowledgeApiKey)
			.timeout(timeoutMs)
			.execute();
		checkKnowledgeResponse(response.getStatus(), response.body());
	}

	/**
	 * 知识库 API 响应校验：HTTP 非 2xx 或响应体携带错误结构均抛异常，杜绝静默失败。
	 * Dify 错误体形如 {"code":"rate_limit_exceeded","message":"...","status":"error"}，code 为字符串，
	 * 不能按数字解析后与 0 比较。
	 */
	private String checkKnowledgeResponse(int httpStatus, String resp) {
		if (httpStatus < 200 || httpStatus >= 300) {
			throw new CheckedException("Dify 知识库 API 调用失败(HTTP " + httpStatus + "): " + StrUtil.subPre(resp, 300));
		}
		JSONObject json = JSONUtil.parseObj(StrUtil.blankToDefault(resp, "{}"));
		String code = json.getStr("code");
		boolean codeError = StrUtil.isNotBlank(code) && !"0".equals(code);
		if ("error".equals(json.getStr("status")) || json.containsKey("error_msg")
				|| (json.getStr("message") != null && codeError)) {
			throw new CheckedException("Dify 知识库 API 调用失败: " + StrUtil.subPre(resp, 300));
		}
		return resp;
	}

	/**
	 * 清洗 LLM 输出：去除推理模型（DeepSeek-R1/QwQ 等）的思考块与 markdown 代码围栏
	 */
	private String cleanLLMOutput(String text) {
		String cleaned = text.trim();
		cleaned = cleaned.replaceAll("(?s)<think>.*?</think>", "").trim();
		cleaned = cleaned.replaceAll("(?s)<think>.*", "").trim();
		if (cleaned.startsWith("```")) {
			cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "").trim();
		}
		if (cleaned.endsWith("```")) {
			cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
		}
		return cleaned;
	}

	/**
	 * 知识库向量检索（语义种子召回）
	 * @param datasetId 数据集 id
	 * @param query 检索文本
	 * @param topK 召回条数
	 * @return records 数组（segment.content 内含【专家ID】标记，score 为相似度）
	 */
	public JSONArray retrieve(String datasetId, String query, int topK) {
		Map<String, Object> retrievalModel = Map.of("search_method", "semantic_search", "reranking_enable", false,
				"top_k", topK, "score_threshold_enabled", false);
		Map<String, Object> body = Map.of("query", query, "retrieval_model", retrievalModel);
		cn.hutool.http.HttpResponse response = HttpRequest.post(knowledgeUrl("/datasets/" + datasetId + "/retrieve"))
			.header("Authorization", "Bearer " + knowledgeApiKey)
			.header("Content-Type", "application/json")
			.body(JSONUtil.toJsonStr(body))
			.timeout(timeoutMs)
			.execute();
		checkKnowledgeResponse(response.getStatus(), response.body());
		JSONArray records = JSONUtil.parseObj(response.body()).getJSONArray("records");
		return records == null ? new JSONArray() : records;
	}
}
