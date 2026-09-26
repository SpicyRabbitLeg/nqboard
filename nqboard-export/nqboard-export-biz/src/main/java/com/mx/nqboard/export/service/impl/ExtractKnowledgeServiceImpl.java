package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.common.core.exception.CheckedException;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.dify.DifyClient;
import com.mx.nqboard.export.service.ExpertService;
import com.mx.nqboard.export.service.ExtractKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家知识库同步 服务实现类
 * </p>
 * <p>
 * 将全量专家画像文本（带学科锚定与【专家ID】标记）写入 Dify 知识库：一专家一片段，
 * 每 500 位专家合并为一个文本文件上传（48 文件/2.4万专家），向量索引由 Dify 异步完成。
 * 未匹配不入库原则同样适用于知识库：专家库变更后需重新同步。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractKnowledgeServiceImpl implements ExtractKnowledgeService {

	/**
	 * 段落分隔符（与 process_rule.segmentation.separator 保持一致，保证一专家一片段）
	 */
	private static final String SEGMENT_SEPARATOR = "\n\n=====\n\n";

	/**
	 * 每个分片文件的专家数
	 */
	private static final int EXPERTS_PER_FILE = 500;

	/**
	 * Dify API 相邻请求间隔（毫秒）：分片连发会触发云端限流
	 */
	private static final long DIFY_REQUEST_INTERVAL_MS = 2000;

	/**
	 * 分片上传失败重试次数
	 */
	private static final int UPLOAD_RETRY = 2;

	/**
	 * 分片上传重试退避间隔（毫秒），覆盖限流窗口
	 */
	private static final long UPLOAD_RETRY_BACKOFF_MS = 20000;

	private final DifyClient difyClient;

	private final ExpertService expertService;

	@Value("${dify.dataset-name:nqboard-专家画像}")
	private String datasetName;

	private final AtomicBoolean syncRunning = new AtomicBoolean(false);

	private final AtomicLong syncDone = new AtomicLong(0);

	private final AtomicLong syncTotal = new AtomicLong(0);

	/**
	 * 同步失败原因（null 表示无错误）
	 */
	private volatile String syncError;

	@Override
	public Map<String, Object> startSync() {
		if (!difyClient.isKnowledgeConfigured()) {
			throw new CheckedException("Dify 知识库未配置，请检查 dify.knowledge-api-key 配置项");
		}
		if (!syncRunning.compareAndSet(false, true)) {
			throw new CheckedException("知识库同步任务正在执行中，请稍后再试");
		}
		syncError = null;
		CompletableFuture.runAsync(() -> {
			try {
				doSync();
			}
			catch (Exception e) {
				log.error("专家知识库同步任务异常终止", e);
				syncError = StrUtil.blankToDefault(e.getMessage(), "知识库同步任务异常终止");
			}
			finally {
				syncRunning.set(false);
			}
		});
		log.info("专家知识库同步任务已启动");
		return Map.of("message", "知识库同步任务已启动");
	}

	@Override
	public Map<String, Object> getProgress() {
		Map<String, Object> progress = new LinkedHashMap<>();
		progress.put("running", syncRunning.get());
		progress.put("done", syncDone.get());
		progress.put("total", syncTotal.get());
		progress.put("error", syncError);
		return progress;
	}

	@Override
	public Map<String, Object> getIndexingStatus() {
		String datasetId = difyClient.findDatasetId(datasetName);
		if (datasetId == null) {
			return Map.of("ready", false, "completed", 0, "total", 0);
		}
		int total = 0;
		int completed = 0;
		int page = 1;
		JSONArray documents;
		do {
			documents = difyClient.listDocuments(datasetId, page, 100);
			if (documents == null) {
				break;
			}
			for (Object item : documents) {
				total++;
				JSONObject doc = (JSONObject) item;
				if ("completed".equals(doc.getStr("indexing_status"))) {
					completed++;
				}
			}
			page++;
		}
		while (documents.size() == 100);
		return Map.of("ready", total > 0 && completed == total, "completed", completed, "total", total);
	}

	/**
	 * 同步主流程：确保数据集 -> 清空旧文档 -> 全量专家画像分片上传
	 */
	private void doSync() {
		String datasetId = ensureDataset();
		// 全量重建：清空旧文档
		clearDocuments(datasetId);

		List<ExpertEntity> experts = expertService.list(Wrappers.<ExpertEntity>lambdaQuery()
			.select(ExpertEntity::getId, ExpertEntity::getExpertName, ExpertEntity::getSubjectCategory,
					ExpertEntity::getFirstDiscipline, ExpertEntity::getResearchDirection, ExpertEntity::getDomainCode,
					ExpertEntity::getDomainName));
		if (CollUtil.isEmpty(experts)) {
			throw new CheckedException("专家库为空，无内容可同步");
		}
		int totalFiles = (experts.size() + EXPERTS_PER_FILE - 1) / EXPERTS_PER_FILE;
		syncTotal.set(totalFiles);
		syncDone.set(0);
		log.info("专家知识库同步开始：数据集 {}({})，专家 {} 人，分片 {} 个", datasetName, datasetId, experts.size(), totalFiles);

		String dataJson = buildProcessRuleJson();
		List<List<ExpertEntity>> batches = CollUtil.split(experts, EXPERTS_PER_FILE);
		for (int i = 0; i < batches.size(); i++) {
			String content = batches.get(i)
				.stream()
				.map(this::buildPictureText)
				.collect(Collectors.joining(SEGMENT_SEPARATOR));
			String fileName = String.format("experts_%03d.txt", i + 1);
			uploadWithRetry(datasetId, fileName, content.getBytes(StandardCharsets.UTF_8), dataJson);
			syncDone.incrementAndGet();
			log.info("知识库分片上传完成 {}/{}：{}（{} 位专家）", i + 1, totalFiles, fileName, batches.get(i).size());
			if (i < batches.size() - 1) {
				throttle();
			}
		}
		log.info("专家知识库同步完成：分片 {}/{}，向量索引由 Dify 后台异步执行", syncDone.get(), syncTotal.get());
	}

	/**
	 * 分片上传带重试退避：单次瞬时失败（如限流）自动重试，重试耗尽抛出异常终止任务
	 */
	private void uploadWithRetry(String datasetId, String fileName, byte[] content, String dataJson) {
		int attempts = 0;
		Exception lastError = null;
		while (attempts <= UPLOAD_RETRY) {
			try {
				difyClient.createDocumentByFile(datasetId, fileName, content, dataJson);
				return;
			}
			catch (Exception e) {
				attempts++;
				lastError = e;
				log.warn("知识库分片上传失败（第 {}/{} 次）：{}，原因：{}", attempts, UPLOAD_RETRY + 1, fileName, e.getMessage());
				if (attempts <= UPLOAD_RETRY) {
					sleep(UPLOAD_RETRY_BACKOFF_MS);
				}
			}
		}
		throw new CheckedException("分片 " + fileName + " 上传失败（已重试 " + UPLOAD_RETRY + " 次）：" + lastError.getMessage(),
				lastError);
	}

	/**
	 * Dify API 请求间节流，避免高频连发触发云端限流
	 */
	private void throttle() {
		sleep(DIFY_REQUEST_INTERVAL_MS);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CheckedException("知识库同步任务被中断");
		}
	}

	private String ensureDataset() {
		String datasetId = difyClient.findDatasetId(datasetName);
		if (datasetId == null) {
			datasetId = difyClient.createDataset(datasetName);
			log.info("已创建 Dify 数据集 {}({})", datasetName, datasetId);
		}
		return datasetId;
	}

	private void clearDocuments(String datasetId) {
		int page = 1;
		boolean first = true;
		JSONArray documents;
		do {
			documents = difyClient.listDocuments(datasetId, page, 100);
			if (documents == null) {
				break;
			}
			for (Object item : documents) {
				if (!first) {
					throttle();
				}
				first = false;
				String documentId = ((JSONObject) item).getStr("id");
				difyClient.deleteDocument(datasetId, documentId);
			}
		}
		while (documents != null && documents.size() == 100);
	}

	/**
	 * 组装专家画像文本：【专家ID】标记供抽取召回后映射回专家，学科上下文用于锚定向量语义
	 */
	private String buildPictureText(ExpertEntity expert) {
		return "【专家ID】" + expert.getId() + "\n" + "专家名称：" + StrUtil.nullToEmpty(expert.getExpertName()) + "\n"
				+ "学科门类：" + StrUtil.nullToEmpty(expert.getSubjectCategory()) + "\n" + "一级学科："
				+ StrUtil.nullToEmpty(expert.getFirstDiscipline()) + "\n" + "研究方向："
				+ StrUtil.nullToEmpty(expert.getResearchDirection()) + "\n" + "所属领域："
				+ (StrUtil.isNotBlank(expert.getDomainName()) ? expert.getDomainName() : "未标注");
	}

	/**
	 * create-by-file 的 data 表单参数：自定义段落分隔符，保证一专家一片段
	 */
	private String buildProcessRuleJson() {
		Map<String, Object> segmentation = new LinkedHashMap<>();
		segmentation.put("separator", SEGMENT_SEPARATOR);
		segmentation.put("max_tokens", 1000);
		Map<String, Object> rules = new LinkedHashMap<>();
		rules.put("pre_processing_rules", List.of(Map.of("id", "remove_extra_spaces", "enabled", true),
				Map.of("id", "remove_urls_emails", "enabled", false)));
		rules.put("segmentation", segmentation);
		Map<String, Object> processRule = new LinkedHashMap<>();
		processRule.put("mode", "custom");
		processRule.put("rules", rules);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("indexing_technique", "high_quality");
		data.put("process_rule", processRule);
		data.put("doc_form", "text_model");
		return JSONUtil.toJsonStr(data);
	}
}
