package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.exception.CheckedException;
import com.mx.nqboard.export.api.dto.ExtractRunDTO;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractDomainEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordDetailEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;
import com.mx.nqboard.export.dify.DifyClient;
import com.mx.nqboard.export.dify.ExtractPrompts;
import com.mx.nqboard.export.service.ExpertService;
import com.mx.nqboard.export.service.ExtractDomainService;
import com.mx.nqboard.export.service.ExtractRecordDetailService;
import com.mx.nqboard.export.service.ExtractRecordService;
import com.mx.nqboard.export.service.ExtractRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家抽取执行 服务实现类
 * </p>
 * <p>
 * 抽取链路：意图解析 -> 池构建（已选/候选）-> 全池向量打分 -> 分档（高分直过/低分送 LLM 复核）-> 落库与未匹配快照。
 * 全程异步执行（单飞防重入，与打标/同步任务同范式），调用方通过 progress 轮询进度。
 * 线程安全：进度字段均为 volatile/Atomic，异步任务仅操作任务内局部变量，无其他共享可变状态。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractRunServiceImpl implements ExtractRunService {

	/**
	 * 档位：1已选、2候选、3复核剔除
	 */
	private static final String GRADE_SELECTED = "1";

	private static final String GRADE_CANDIDATE = "2";

	private static final String GRADE_REJECTED = "3";

	/**
	 * 记录状态：0成功、1失败、2运行中
	 */
	private static final String STATUS_SUCCESS = "0";

	private static final String STATUS_FAILED = "1";

	private static final String STATUS_RUNNING = "2";

	/**
	 * 向量语义种子召回条数（打分检索结果的前 N 名视为种子区间）
	 */
	private static final int VECTOR_SEED_TOP_K = 100;

	/**
	 * 候选数量上限
	 */
	private static final int CANDIDATE_MAX = 2000;

	/**
	 * 打分检索 topK 上限：rerank 模式下单次检索条数硬限制，超过约 500 Dify 会静默返回空结果（HTTP 200 + 0 条，2026-09-27 实测）；
	 * 未被覆盖打分的池内成员按设计落入 LLM 复核带
	 */
	private static final int SCORE_TOP_K_CAP = 500;

	/**
	 * 知识库片段中的专家标记
	 */
	private static final Pattern EXPERT_ID_PATTERN = Pattern.compile("【专家ID】(\\d+)");

	private final DifyClient difyClient;

	private final ExpertService expertService;

	private final ExtractDomainService extractDomainService;

	private final ExtractRecordService extractRecordService;

	private final ExtractRecordDetailService extractRecordDetailService;

	private final TransactionTemplate transactionTemplate;

	@Value("${dify.dataset-name:nqboard-专家画像}")
	private String datasetName;

	/**
	 * 高分直过阈值：≥ 该值的池内专家免 LLM 复核（评测集标定后调整）
	 */
	@Value("${extract.score-keep-threshold:0.45}")
	private BigDecimal keepThreshold;

	/**
	 * LLM 复核每批人数
	 */
	@Value("${extract.review-batch-size:30}")
	private int reviewBatchSize;

	/**
	 * 复核批次并发数（在 Dify/LLM 服务端并发容量内调大，可近线性缩短复核墙钟时间）
	 */
	@Value("${extract.review-concurrency:4}")
	private int reviewConcurrency;

	/**
	 * 抽取任务运行标记（防并发重入）
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicLong progressDone = new AtomicLong(0);

	private final AtomicLong progressTotal = new AtomicLong(0);

	/**
	 * 当前阶段：parse/score/review/save
	 */
	private volatile String progressStage;

	/**
	 * 当前任务的记录id（完成后保留，供前端跳转）
	 */
	private volatile Long progressRecordId;

	/**
	 * 失败原因（null 表示无错误）
	 */
	private volatile String progressError;

	@Override
	public ExtractRecordEntity run(ExtractRunDTO dto) {
		String query = dto.getQuery().trim();
		long start = System.currentTimeMillis();
		// 同步快失败：领域清单为空时任务必然无产出，前置校验让调用方即时感知
		List<ExtractDomainEntity> domains = extractDomainService.listDomains("0");
		if (CollUtil.isEmpty(domains)) {
			throw new CheckedException("领域清单为空，请先在领域管理中完成 AI 归纳与审核");
		}
		if (!running.compareAndSet(false, true)) {
			throw new CheckedException("专家抽取任务正在执行中，请稍后再试");
		}
		// 预建运行中记录，id 立即可用，任务结果按此 id 落库
		ExtractRecordEntity record = new ExtractRecordEntity();
		record.setQueryText(query);
		record.setStatus(STATUS_RUNNING);
		extractRecordService.save(record);
		progressRecordId = record.getId();
		progressStage = "parse";
		progressError = null;
		progressDone.set(0);
		progressTotal.set(0);
		CompletableFuture.runAsync(() -> {
			try {
				doRun(record, query, domains, start);
			}
			catch (Exception e) {
				log.error("专家抽取任务异常终止：recordId={}", record.getId(), e);
				progressError = StrUtil.blankToDefault(e.getMessage(), "抽取任务异常终止");
				failRecord(record, e, start);
			}
			finally {
				running.set(false);
			}
		});
		log.info("专家抽取任务已启动：recordId={}，query={}", record.getId(), query);
		return record;
	}

	@Override
	public Map<String, Object> progress() {
		Map<String, Object> progress = new LinkedHashMap<>();
		progress.put("running", running.get());
		progress.put("stage", progressStage);
		progress.put("done", progressDone.get());
		progress.put("total", progressTotal.get());
		progress.put("recordId", progressRecordId);
		progress.put("error", progressError);
		return progress;
	}

	/**
	 * 抽取主流程：意图解析 -> 池构建 -> 全池向量打分 -> 分档复核 -> 落库与未匹配快照
	 */
	private void doRun(ExtractRecordEntity record, String query, List<ExtractDomainEntity> domains, long start) {
		Map<String, ExtractDomainEntity> byCode = domains.stream()
			.collect(Collectors.toMap(ExtractDomainEntity::getDomainCode, Function.identity(), (a, b) -> a));
		String domainListJson = JSONUtil.toJsonStr(domains.stream()
			.map(d -> Map.of("domain_code", d.getDomainCode(), "domain_name", StrUtil.nullToEmpty(d.getDomainName()),
					"description", StrUtil.nullToEmpty(d.getDescription()), "keywords", StrUtil.nullToEmpty(d.getKeywords())))
			.toList());
		// 1. 意图解析：query -> 领域 + 关键词
		JSONObject parsed = JSONUtil
			.parseObj(difyClient.runLLMTask("parse", ExtractPrompts.parse(domainListJson, query)));
		List<String> parsedCodes = parseCodes(parsed.getJSONArray("domains"), byCode.keySet());
		List<String> keywords = parseStringArray(parsed.getJSONArray("keywords"));
		// 2. 已选：领域精确命中（标签主路径，不受向量误召回影响）
		Map<Long, ExpertEntity> poolExperts = new LinkedHashMap<>();
		List<ExtractRecordDetailEntity> pool = new ArrayList<>();
		int selectedBase;
		if (!parsedCodes.isEmpty()) {
			List<ExpertEntity> selected = expertService
				.list(Wrappers.<ExpertEntity>lambdaQuery().in(ExpertEntity::getDomainCode, parsedCodes));
			selectedBase = selected.size();
			selected.forEach(e -> {
				poolExperts.put(e.getId(), e);
				pool.add(buildDetail(e, GRADE_SELECTED, "领域精确命中"));
			});
		}
		else {
			selectedBase = 0;
		}
		// 3. 候选：邻接领域（主），剔除已选，上限截断
		Set<String> adjacentCodes = collectAdjacent(parsedCodes, byCode);
		adjacentCodes.removeAll(parsedCodes);
		int adjacentCount = 0;
		if (!adjacentCodes.isEmpty()) {
			List<ExpertEntity> adjacentExperts = expertService
				.list(Wrappers.<ExpertEntity>lambdaQuery().in(ExpertEntity::getDomainCode, adjacentCodes));
			adjacentCount = Math.min(adjacentExperts.size(), CANDIDATE_MAX);
			adjacentExperts.stream().limit(CANDIDATE_MAX).forEach(e -> {
				poolExperts.put(e.getId(), e);
				pool.add(buildDetail(e, GRADE_CANDIDATE, "邻接领域命中"));
			});
		}
		// 4. 大 top_k 检索：为全池打分，同时前 N 名提供向量语义种子（截断上限内）；
		// 超长多主题查询按方向词切分为多探针分别检索，消除 250 字符截断对清单尾部方向的打分偏差
		progressStage = "score";
		List<String> probes = buildSearchProbes(query, keywords);
		int topK = Math.min(pool.size() + VECTOR_SEED_TOP_K + 50, SCORE_TOP_K_CAP);
		String datasetId = difyClient.findDatasetId(datasetName);
		LinkedHashMap<Long, BigDecimal> ranked;
		if (datasetId == null) {
			log.warn("Dify 知识库数据集 {} 不存在，跳过向量打分与语义种子召回", datasetName);
			ranked = new LinkedHashMap<>();
		}
		else {
			ranked = retrieveRanked(datasetId, probes, topK);
		}
		appendVectorSeeds(ranked, poolExperts, pool, adjacentCount);
		pool.forEach(d -> d.setScore(ranked.get(d.getExpertId())));
		// 5. 分档：高分直过，其余送 LLM 复核
		List<ExtractRecordDetailEntity> reviewBand = splitReviewBand(pool, keepThreshold);
		reviewBatch(query, reviewBand, poolExperts);
		// 6. 落库（记录 + 已选/候选/复核剔除明细）与未匹配快照（复核后口径才完整）
		progressStage = "save";
		List<Long> keptCandidateIds = pool.stream()
			.filter(d -> GRADE_CANDIDATE.equals(d.getGrade()))
			.map(ExtractRecordDetailEntity::getExpertId)
			.toList();
		List<Long> rejectedIds = pool.stream()
			.filter(d -> GRADE_REJECTED.equals(d.getGrade()))
			.map(ExtractRecordDetailEntity::getExpertId)
			.toList();
		long unmatched = countUnmatched(parsedCodes, keptCandidateIds, rejectedIds);
		record.setParsedDomains(JSONUtil.toJsonStr(parsedCodes));
		record.setParsedKeywords(JSONUtil.toJsonStr(keywords));
		record.setKeepThreshold(keepThreshold);
		record.setSelectedCount((int) pool.stream().filter(d -> GRADE_SELECTED.equals(d.getGrade())).count());
		record.setCandidateCount(keptCandidateIds.size());
		record.setUnmatchedCount((int) Math.min(unmatched, Integer.MAX_VALUE));
		record.setCostMs((int) (System.currentTimeMillis() - start));
		record.setStatus(STATUS_SUCCESS);
		transactionTemplate.executeWithoutResult(status -> {
			extractRecordService.updateById(record);
			pool.forEach(d -> d.setRecordId(record.getId()));
			if (CollUtil.isNotEmpty(pool)) {
				extractRecordDetailService.saveBatch(pool);
			}
		});
		log.info("专家抽取完成：recordId={}，query={}，已选 {} 候选 {} 复核剔除 {} 未匹配 {}，耗时 {}ms", record.getId(), query,
				record.getSelectedCount(), record.getCandidateCount(), rejectedIds.size(), unmatched, record.getCostMs());
	}

	/**
	 * 未匹配专家动态分页：库内其余专家（不属于记录解析领域、不在存活候选名单）∪ LLM 复核剔除者
	 */
	@Override
	public Page<ExpertEntity> pageUnmatched(Page<ExpertEntity> page, Long recordId) {
		ExtractRecordEntity record = extractRecordService.getById(recordId);
		if (record == null) {
			throw new CheckedException("抽取记录不存在: " + recordId);
		}
		List<String> parsedCodes = parseStringArray(
				JSONUtil.parseArray(StrUtil.blankToDefault(record.getParsedDomains(), "[]")));
		// 存活候选与复核剔除名单（grade 2/3；已选由领域过滤覆盖，无需另查）
		List<ExtractRecordDetailEntity> details = extractRecordDetailService
			.list(Wrappers.<ExtractRecordDetailEntity>lambdaQuery()
				.select(ExtractRecordDetailEntity::getGrade, ExtractRecordDetailEntity::getExpertId)
				.eq(ExtractRecordDetailEntity::getRecordId, recordId)
				.in(ExtractRecordDetailEntity::getGrade, GRADE_CANDIDATE, GRADE_REJECTED));
		List<Long> keptCandidateIds = details.stream()
			.filter(d -> GRADE_CANDIDATE.equals(d.getGrade()))
			.map(ExtractRecordDetailEntity::getExpertId)
			.toList();
		List<Long> rejectedIds = details.stream()
			.filter(d -> GRADE_REJECTED.equals(d.getGrade()))
			.map(ExtractRecordDetailEntity::getExpertId)
			.toList();
		// 分页规范：先分页查 id（domain 列索引），再按 id in 查详情
		LambdaQueryWrapper<ExpertEntity> idWrapper = buildUnmatchedWrapper(parsedCodes, keptCandidateIds, rejectedIds)
			.select(ExpertEntity::getId);
		if (CollUtil.isEmpty(page.orders())) {
			idWrapper.orderByDesc(ExpertEntity::getId);
		}
		Page<ExpertEntity> idPage = expertService.page(page, idWrapper);
		List<Long> ids = idPage.getRecords().stream().map(ExpertEntity::getId).toList();
		if (CollUtil.isEmpty(ids)) {
			idPage.setRecords(Collections.emptyList());
			return idPage;
		}
		Map<Long, ExpertEntity> detailMap = expertService.listByIds(ids).stream()
			.collect(Collectors.toMap(ExpertEntity::getId, Function.identity()));
		idPage.setRecords(ids.stream().map(detailMap::get).filter(Objects::nonNull).toList());
		return idPage;
	}

	/**
	 * 意图解析结果中的领域编码过滤（仅保留清单内合法编码）
	 */
	private List<String> parseCodes(JSONArray array, Set<String> validCodes) {
		if (array == null) {
			return Collections.emptyList();
		}
		return array.stream().map(String::valueOf).filter(validCodes::contains).distinct().toList();
	}

	private List<String> parseStringArray(JSONArray array) {
		if (array == null) {
			return Collections.emptyList();
		}
		return array.stream().map(String::valueOf).filter(StrUtil::isNotBlank).distinct().toList();
	}

	/**
	 * 汇总解析领域的邻接领域编码（候选扩展范围）
	 */
	private Set<String> collectAdjacent(List<String> parsedCodes, Map<String, ExtractDomainEntity> byCode) {
		Set<String> adjacent = new HashSet<>();
		for (String code : parsedCodes) {
			ExtractDomainEntity domain = byCode.get(code);
			if (domain == null || StrUtil.isBlank(domain.getAdjacentCodes())) {
				continue;
			}
			try {
				JSONUtil.parseArray(domain.getAdjacentCodes()).forEach(c -> adjacent.add(String.valueOf(c)));
			}
			catch (Exception e) {
				log.warn("邻接领域编码解析失败: {}", domain.getAdjacentCodes());
			}
		}
		return adjacent;
	}

	/**
	 * 构建检索探针：短查询（≤Dify 250 字符上限）保持原口径单探针（query+keywords，与标定口径一致）；
	 * 超长查询（如方向清单）按顿号等分隔符切分方向词、贪心打包为多个 ≤250 字符的探针，关键词单独成探针，
	 * 保证清单尾部方向同样参与向量打分，消除截断偏差
	 */
	static List<String> buildSearchProbes(String query, List<String> keywords) {
		String full = keywords.isEmpty() ? query : query + " " + String.join(" ", keywords);
		if (full.length() <= DifyClient.RETRIEVE_QUERY_MAX_CHARS) {
			return List.of(full);
		}
		List<String> probes = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String raw : query.split("[、，,；;。\\s]+")) {
			String token = raw.trim();
			if (token.isEmpty()) {
				continue;
			}
			// 单个方向词仍超上限时按字符硬切兜底
			while (token.length() > DifyClient.RETRIEVE_QUERY_MAX_CHARS) {
				if (current.length() > 0) {
					probes.add(current.toString());
					current = new StringBuilder();
				}
				probes.add(token.substring(0, DifyClient.RETRIEVE_QUERY_MAX_CHARS));
				token = token.substring(DifyClient.RETRIEVE_QUERY_MAX_CHARS);
			}
			if (current.length() + token.length() + 1 > DifyClient.RETRIEVE_QUERY_MAX_CHARS && current.length() > 0) {
				probes.add(current.toString());
				current = new StringBuilder();
			}
			if (current.length() > 0) {
				current.append(" ");
			}
			current.append(token);
		}
		if (current.length() > 0) {
			probes.add(current.toString());
		}
		if (!keywords.isEmpty()) {
			probes.add(String.join(" ", keywords));
		}
		return probes;
	}

	/**
	 * 多探针知识库检索并合并为 专家id -> 最高相似度 的降序映射（检索返回按相关度降序）：
	 * 多主题查询同一专家取各探针得分的最大值——命中任一方向即算强相关
	 */
	private LinkedHashMap<Long, BigDecimal> retrieveRanked(String datasetId, List<String> probes, int topK) {
		Map<Long, BigDecimal> merged = new HashMap<>();
		for (String probe : probes) {
			for (Object item : difyClient.retrieve(datasetId, probe, topK)) {
				JSONObject record = (JSONObject) item;
				JSONObject segment = record.getJSONObject("segment");
				String content = segment == null ? null : segment.getStr("content");
				if (StrUtil.isBlank(content)) {
					continue;
				}
				Matcher matcher = EXPERT_ID_PATTERN.matcher(content);
				if (!matcher.find()) {
					continue;
				}
				BigDecimal score = record.getBigDecimal("score");
				if (score == null) {
					continue;
				}
				merged.merge(Long.valueOf(matcher.group(1)), score, BigDecimal::max);
			}
		}
		return merged.entrySet()
			.stream()
			.sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/**
	 * 向量语义种子：打分检索结果的前 N 名中不属于已选/邻接候选的专家，补充为候选（兜底领域标签未覆盖的语义匹配）。
	 * 种子受候选上限余量约束：邻接候选占满 CANDIDATE_MAX 时不再补充种子。
	 */
	private void appendVectorSeeds(LinkedHashMap<Long, BigDecimal> ranked, Map<Long, ExpertEntity> poolExperts,
			List<ExtractRecordDetailEntity> pool, int adjacentCount) {
		int seedBudget = Math.max(0, CANDIDATE_MAX - adjacentCount);
		if (ranked.isEmpty() || seedBudget == 0) {
			return;
		}
		int rank = 0;
		List<Long> seedIds = new ArrayList<>();
		for (Map.Entry<Long, BigDecimal> entry : ranked.entrySet()) {
			if (rank++ >= VECTOR_SEED_TOP_K || seedIds.size() >= seedBudget) {
				break;
			}
			if (!poolExperts.containsKey(entry.getKey())) {
				seedIds.add(entry.getKey());
			}
		}
		if (seedIds.isEmpty()) {
			return;
		}
		Map<Long, ExpertEntity> seedMap = expertService.listByIds(seedIds).stream()
			.collect(Collectors.toMap(ExpertEntity::getId, Function.identity(), (a, b) -> a));
		for (Long expertId : seedIds) {
			ExpertEntity expert = seedMap.get(expertId);
			if (expert == null) {
				continue;
			}
			poolExperts.put(expertId, expert);
			ExtractRecordDetailEntity detail = buildDetail(expert, GRADE_CANDIDATE, "向量语义召回");
			detail.setScore(ranked.get(expertId));
			pool.add(detail);
		}
	}

	/**
	 * LLM 分批复核（批次并发执行）：relevant=false 判 grade=3 复核剔除；批次失败按保留兜底（宁可误留不误杀）；
	 * 输出未覆盖（index 缺失/越界/漏条）的成员追加一轮补审，补审仍未覆盖的标注保留原因。
	 * 线程安全：批次间各自操作明细子列表（无共享可变状态），进度为 Atomic/volatile，DifyClient 无状态
	 */
	void reviewBatch(String query, List<ExtractRecordDetailEntity> reviewBand, Map<Long, ExpertEntity> poolExperts) {
		if (reviewBand.isEmpty()) {
			return;
		}
		progressStage = "review";
		progressTotal.set(reviewBand.size());
		progressDone.set(0);
		List<List<ExtractRecordDetailEntity>> batches = CollUtil.split(reviewBand, reviewBatchSize);
		ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, reviewConcurrency));
		try {
			CompletableFuture.allOf(batches.stream()
				.<CompletableFuture<Void>>map(
					batch -> CompletableFuture.runAsync(() -> reviewBatchWithRetry(query, batch, poolExperts), executor))
				.toArray(CompletableFuture[]::new)).join();
		}
		finally {
			executor.shutdown();
		}
	}

	/**
	 * 单批复核 + 未覆盖补审。复核未完成（批次失败/补审仍未覆盖）的已选人降档为候选：
	 * 已选语义=领域命中且经 rerank 高分或 LLM 确认，未经验证者不应留在已选
	 */
	private void reviewBatchWithRetry(String query, List<ExtractRecordDetailEntity> batch,
			Map<Long, ExpertEntity> poolExperts) {
		List<String> originalReasons = batch.stream().map(ExtractRecordDetailEntity::getReason).toList();
		reviewOneBatch(query, batch, poolExperts);
		List<ExtractRecordDetailEntity> uncovered = uncoveredOf(batch, originalReasons);
		if (!uncovered.isEmpty()) {
			log.warn("LLM 复核输出未覆盖 {} 名成员，追加一轮补审", uncovered.size());
			List<String> retryBaseline = uncovered.stream().map(ExtractRecordDetailEntity::getReason).toList();
			reviewOneBatch(query, uncovered, poolExperts);
			uncoveredOf(uncovered, retryBaseline).forEach(this::markUnverifiedCandidate);
		}
		progressDone.addAndGet(batch.size());
	}

	/**
	 * 未经验证的已选人降档为候选并标注原因
	 */
	private void markUnverifiedCandidate(ExtractRecordDetailEntity d) {
		if (GRADE_SELECTED.equals(d.getGrade())) {
			d.setGrade(GRADE_CANDIDATE);
		}
		d.setReason(d.getReason() + "，复核未覆盖保留");
	}

	/**
	 * 单批复核：调 LLM 并应用结果；输出非 JSON 数组（对象包裹/散文）重试一次；仍失败整批保留兜底，
	 * 兜底保留的已选人同样降档为候选
	 */
	private void reviewOneBatch(String query, List<ExtractRecordDetailEntity> batch, Map<Long, ExpertEntity> poolExperts) {
		try {
			String payload = ExtractPrompts.review(query, JSONUtil.toJsonStr(buildReviewRows(batch, poolExperts)));
			JSONArray array;
			try {
				array = parseReviewArray(difyClient.runLLMTask("review", payload));
			}
			catch (Exception parseError) {
				log.warn("LLM 复核输出解析失败，重试一次: {}", parseError.getMessage());
				array = parseReviewArray(difyClient.runLLMTask("review", payload));
			}
			applyReviewResult(batch, array);
		}
		catch (Exception e) {
			log.error("LLM 复核批次失败，{} 名专家按保留兜底", batch.size(), e);
			batch.forEach(d -> {
				if (GRADE_SELECTED.equals(d.getGrade())) {
					d.setGrade(GRADE_CANDIDATE);
				}
				d.setReason(d.getReason() + "，复核异常保留");
			});
		}
	}

	private List<Map<String, Object>> buildReviewRows(List<ExtractRecordDetailEntity> batch,
			Map<Long, ExpertEntity> poolExperts) {
		List<Map<String, Object>> indexed = new ArrayList<>();
		for (int i = 0; i < batch.size(); i++) {
			ExtractRecordDetailEntity d = batch.get(i);
			ExpertEntity expert = poolExperts.get(d.getExpertId());
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("index", i + 1);
			row.put("expert_name", StrUtil.nullToEmpty(d.getExpertName()));
			row.put("subject_category", StrUtil.nullToEmpty(d.getSubjectCategory()));
			row.put("first_discipline", StrUtil.nullToEmpty(d.getFirstDiscipline()));
			row.put("research_direction", StrUtil.nullToEmpty(expert == null ? null : expert.getResearchDirection()));
			row.put("domain_name", StrUtil.nullToEmpty(expert == null ? null : expert.getDomainName()));
			row.put("score", d.getScore() == null ? "" : d.getScore().toPlainString());
			indexed.add(row);
		}
		return indexed;
	}

	/**
	 * 解析复核输出：优先按 JSON 数组解析；模型偶发把数组包进 JSON 对象（如 {"result":[...]}）时提取内层数组；
	 * 仍无法解析抛异常，交由调用方重试/兜底
	 */
	private JSONArray parseReviewArray(String result) {
		String cleaned = StrUtil.trim(result);
		if (StrUtil.isNotEmpty(cleaned) && cleaned.charAt(0) == '[') {
			return JSONUtil.parseArray(cleaned);
		}
		try {
			JSONObject obj = JSONUtil.parseObj(cleaned);
			for (String key : List.of("result", "data", "items", "list")) {
				Object value = obj.get(key);
				if (value instanceof JSONArray && !((JSONArray) value).isEmpty()) {
					return (JSONArray) value;
				}
			}
			for (String key : obj.keySet()) {
				Object value = obj.get(key);
				if (value instanceof JSONArray && !((JSONArray) value).isEmpty()) {
					return (JSONArray) value;
				}
			}
		}
		catch (Exception e) {
			log.warn("LLM 复核输出容错解析失败: {}", StrUtil.subPre(cleaned, 120));
		}
		throw new CheckedException("LLM 复核输出不是 JSON 数组: " + StrUtil.subPre(cleaned, 120));
	}

	/**
	 * 找出复核后理由未变化的成员（LLM 输出未覆盖：index 缺失/越界/漏条）
	 */
	private List<ExtractRecordDetailEntity> uncoveredOf(List<ExtractRecordDetailEntity> batch, List<String> beforeReasons) {
		List<ExtractRecordDetailEntity> uncovered = new ArrayList<>();
		for (int i = 0; i < batch.size(); i++) {
			if (beforeReasons.get(i).equals(batch.get(i).getReason())) {
				uncovered.add(batch.get(i));
			}
		}
		return uncovered;
	}

	/**
	 * 分档：score ≥ 阈值的直接保留（理由追加"向量高分"），其余（低分与低于检索底线的无分）进入复核带
	 * @return 复核带明细
	 */
	static List<ExtractRecordDetailEntity> splitReviewBand(List<ExtractRecordDetailEntity> pool, BigDecimal threshold) {
		List<ExtractRecordDetailEntity> reviewBand = new ArrayList<>();
		for (ExtractRecordDetailEntity d : pool) {
			if (d.getScore() != null && d.getScore().compareTo(threshold) >= 0) {
				d.setReason(d.getReason() + "，向量高分");
			}
			else {
				reviewBand.add(d);
			}
		}
		return reviewBand;
	}

	/**
	 * 应用复核结果（防御性解析）：relevant=false 判 grade=3 复核剔除；true 或缺省视为保留；
	 * index 缺失或越界的输出丢弃。JSON 解析异常由调用方按批次兜底处理。
	 */
	static void applyReviewResult(List<ExtractRecordDetailEntity> batch, JSONArray array) {
		for (Object item : array) {
			if (!(item instanceof JSONObject)) {
				continue;
			}
			JSONObject obj = (JSONObject) item;
			Integer index = obj.getInt("index");
			if (index == null || index < 1 || index > batch.size()) {
				continue;
			}
			ExtractRecordDetailEntity d = batch.get(index - 1);
			if (Boolean.FALSE.equals(obj.getBool("relevant"))) {
				d.setGrade(GRADE_REJECTED);
				d.setReason(StrUtil.subPre("LLM复核剔除：" + StrUtil.nullToDefault(obj.getStr("reason"), "与查询意图不相关"), 255));
			}
			else {
				d.setReason(d.getReason() + "，LLM复核通过");
			}
		}
	}

	/**
	 * 未匹配口径（count 与分页单一来源）：库内其余专家（不属于解析领域且不在存活候选名单）∪ LLM 复核剔除者
	 */
	private LambdaQueryWrapper<ExpertEntity> buildUnmatchedWrapper(List<String> parsedCodes, List<Long> keptCandidateIds,
			List<Long> rejectedIds) {
		boolean hasMainBranch = !parsedCodes.isEmpty() || !keptCandidateIds.isEmpty();
		if (rejectedIds.isEmpty() && !hasMainBranch) {
			// 查询一无所获时未匹配=全库
			return Wrappers.lambdaQuery();
		}
		return Wrappers.<ExpertEntity>lambdaQuery().and(w -> {
			if (!rejectedIds.isEmpty()) {
				w.in(ExpertEntity::getId, rejectedIds);
			}
			if (hasMainBranch) {
				if (!rejectedIds.isEmpty()) {
					w.or();
				}
				w.nested(w2 -> {
					if (!parsedCodes.isEmpty()) {
						w2.and(x -> x.isNull(ExpertEntity::getDomainCode).or().notIn(ExpertEntity::getDomainCode, parsedCodes));
					}
					w2.notIn(!keptCandidateIds.isEmpty(), ExpertEntity::getId, keptCandidateIds);
				});
			}
		});
	}

	private long countUnmatched(List<String> parsedCodes, List<Long> keptCandidateIds, List<Long> rejectedIds) {
		return expertService.count(buildUnmatchedWrapper(parsedCodes, keptCandidateIds, rejectedIds));
	}

	private ExtractRecordDetailEntity buildDetail(ExpertEntity expert, String grade, String reason) {
		ExtractRecordDetailEntity detail = new ExtractRecordDetailEntity();
		detail.setExpertId(expert.getId());
		detail.setExpertName(expert.getExpertName());
		detail.setSubjectCategory(expert.getSubjectCategory());
		detail.setFirstDiscipline(expert.getFirstDiscipline());
		detail.setGrade(grade);
		detail.setReason(reason);
		return detail;
	}

	/**
	 * 异步任务失败：预建记录更新为失败态（替代旧版另插失败记录）
	 */
	private void failRecord(ExtractRecordEntity record, Exception e, long start) {
		try {
			record.setStatus(STATUS_FAILED);
			record.setFailReason(StrUtil.subPre(StrUtil.blankToDefault(e.getMessage(), "抽取任务异常终止"), 490));
			record.setCostMs((int) (System.currentTimeMillis() - start));
			extractRecordService.updateById(record);
		}
		catch (Exception ex) {
			log.error("失败抽取记录更新异常", ex);
		}
	}
}
