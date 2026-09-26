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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家抽取执行 服务实现类
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
	 * 档位：1已选、2候选
	 */
	private static final String GRADE_SELECTED = "1";

	private static final String GRADE_CANDIDATE = "2";

	/**
	 * 向量语义种子召回条数（Dify 知识库 top_k）
	 */
	private static final int VECTOR_SEED_TOP_K = 100;

	/**
	 * 候选数量上限
	 */
	private static final int CANDIDATE_MAX = 2000;

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

	@Override
	public ExtractRecordEntity run(ExtractRunDTO dto) {
		String query = dto.getQuery().trim();
		long start = System.currentTimeMillis();
		List<ExtractDomainEntity> domains = extractDomainService.listDomains("0");
		if (CollUtil.isEmpty(domains)) {
			throw new CheckedException("领域清单为空，请先在领域管理中完成 AI 归纳与审核");
		}
		Map<String, ExtractDomainEntity> byCode = domains.stream()
			.collect(Collectors.toMap(ExtractDomainEntity::getDomainCode, Function.identity(), (a, b) -> a));
		String domainListJson = JSONUtil.toJsonStr(domains.stream()
			.map(d -> Map.of("domain_code", d.getDomainCode(), "domain_name", StrUtil.nullToEmpty(d.getDomainName()),
					"description", StrUtil.nullToEmpty(d.getDescription()), "keywords", StrUtil.nullToEmpty(d.getKeywords())))
			.toList());
		try {
			// 1. 意图解析：query -> 领域 + 关键词
			JSONObject parsed = JSONUtil
				.parseObj(difyClient.runLLMTask("parse", ExtractPrompts.parse(domainListJson, query)));
			List<String> parsedCodes = parseCodes(parsed.getJSONArray("domains"), byCode.keySet());
			List<String> keywords = parseStringArray(parsed.getJSONArray("keywords"));
			// 2. 已选：领域精确命中（标签主路径，不受向量误召回影响）
			Map<Long, ExpertEntity> selectedMap = new LinkedHashMap<>();
			if (!parsedCodes.isEmpty()) {
				expertService.list(Wrappers.<ExpertEntity>lambdaQuery().in(ExpertEntity::getDomainCode, parsedCodes))
					.forEach(e -> selectedMap.put(e.getId(), e));
			}
			// 3. 候选：邻接领域（主）+ 向量语义种子（辅），剔除已选、去重、上限截断
			LinkedHashMap<Long, ExtractRecordDetailEntity> candidateMap = new LinkedHashMap<>();
			Set<Long> selectedIds = selectedMap.keySet();
			Set<String> adjacentCodes = collectAdjacent(parsedCodes, byCode);
			adjacentCodes.removeAll(parsedCodes);
			if (!adjacentCodes.isEmpty()) {
				expertService.list(Wrappers.<ExpertEntity>lambdaQuery().in(ExpertEntity::getDomainCode, adjacentCodes))
					.forEach(e -> candidateMap.put(e.getId(), buildDetail(e, GRADE_CANDIDATE, null, "邻接领域命中")));
			}
			appendVectorSeeds(candidateMap, selectedIds, query);
			List<ExtractRecordDetailEntity> candidateList = candidateMap.values()
				.stream()
				.limit(CANDIDATE_MAX)
				.collect(Collectors.toList());
			Set<Long> candidateIds = candidateList.stream()
				.map(ExtractRecordDetailEntity::getExpertId)
				.collect(Collectors.toSet());
			// 4. 未匹配数量快照（明细表不存未匹配，前端按记录动态分页查询）
			long unmatched = countUnmatched(parsedCodes, candidateIds);
			// 5. 落库（记录 + 已选/候选明细）
			ExtractRecordEntity record = new ExtractRecordEntity();
			record.setQueryText(query);
			record.setParsedDomains(JSONUtil.toJsonStr(parsedCodes));
			record.setParsedKeywords(JSONUtil.toJsonStr(keywords));
			record.setSelectedCount(selectedMap.size());
			record.setCandidateCount(candidateList.size());
			record.setUnmatchedCount((int) Math.min(unmatched, Integer.MAX_VALUE));
			record.setCostMs((int) (System.currentTimeMillis() - start));
			record.setStatus("0");
			List<ExtractRecordDetailEntity> details = new ArrayList<>();
			selectedMap.values().forEach(e -> details.add(buildDetail(e, GRADE_SELECTED, null, "领域精确命中")));
			details.addAll(candidateList);
			transactionTemplate.executeWithoutResult(status -> {
				extractRecordService.save(record);
				details.forEach(d -> d.setRecordId(record.getId()));
				extractRecordDetailService.saveBatch(details);
			});
			log.info("专家抽取完成：query={}，已选 {} 候选 {} 未匹配 {}，耗时 {}ms", query, selectedMap.size(),
					candidateList.size(), unmatched, record.getCostMs());
			return record;
		}
		catch (RuntimeException e) {
			saveFailedRecord(query, e, (int) (System.currentTimeMillis() - start));
			throw e;
		}
	}

	/**
	 * 未匹配专家动态分页：专家库中不属于记录解析领域、且不在该记录候选名单内的专家
	 */
	@Override
	public Page<ExpertEntity> pageUnmatched(Page<ExpertEntity> page, Long recordId) {
		ExtractRecordEntity record = extractRecordService.getById(recordId);
		if (record == null) {
			throw new CheckedException("抽取记录不存在: " + recordId);
		}
		List<String> parsedCodes = parseStringArray(
				JSONUtil.parseArray(StrUtil.blankToDefault(record.getParsedDomains(), "[]")));
		List<Long> candidateIds = extractRecordDetailService
			.list(Wrappers.<ExtractRecordDetailEntity>lambdaQuery()
				.select(ExtractRecordDetailEntity::getExpertId)
				.eq(ExtractRecordDetailEntity::getRecordId, recordId)
				.eq(ExtractRecordDetailEntity::getGrade, GRADE_CANDIDATE))
			.stream()
			.map(ExtractRecordDetailEntity::getExpertId)
			.toList();
		// 分页规范：先分页查 id（domain 列索引），再按 id in 查详情
		LambdaQueryWrapper<ExpertEntity> idWrapper = Wrappers.<ExpertEntity>lambdaQuery().select(ExpertEntity::getId);
		if (!parsedCodes.isEmpty()) {
			idWrapper.and(w -> w.isNull(ExpertEntity::getDomainCode).or().notIn(ExpertEntity::getDomainCode, parsedCodes));
		}
		if (!candidateIds.isEmpty()) {
			idWrapper.notIn(ExpertEntity::getId, candidateIds);
		}
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
	 * 向量语义种子：知识库召回片段中解析专家ID，补充领域标签未覆盖的语义匹配专家
	 */
	private void appendVectorSeeds(LinkedHashMap<Long, ExtractRecordDetailEntity> candidateMap, Set<Long> selectedIds,
			String query) {
		String datasetId = difyClient.findDatasetId(datasetName);
		if (datasetId == null) {
			log.warn("Dify 知识库数据集 {} 不存在，跳过向量语义种子召回", datasetName);
			return;
		}
		JSONArray records = difyClient.retrieve(datasetId, query, VECTOR_SEED_TOP_K);
		List<Long> seedIds = new ArrayList<>();
		Map<Long, BigDecimal> scores = new LinkedHashMap<>();
		for (Object item : records) {
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
			Long expertId = Long.valueOf(matcher.group(1));
			if (selectedIds.contains(expertId) || candidateMap.containsKey(expertId) || seedIds.contains(expertId)) {
				continue;
			}
			seedIds.add(expertId);
			scores.put(expertId, record.getBigDecimal("score"));
		}
		if (seedIds.isEmpty()) {
			return;
		}
		expertService.listByIds(seedIds).forEach(e -> candidateMap.put(e.getId(),
				buildDetail(e, GRADE_CANDIDATE, scores.getOrDefault(e.getId(), null), "向量语义召回")));
	}

	private ExtractRecordDetailEntity buildDetail(ExpertEntity expert, String grade, BigDecimal score, String reason) {
		ExtractRecordDetailEntity detail = new ExtractRecordDetailEntity();
		detail.setExpertId(expert.getId());
		detail.setExpertName(expert.getExpertName());
		detail.setSubjectCategory(expert.getSubjectCategory());
		detail.setFirstDiscipline(expert.getFirstDiscipline());
		detail.setGrade(grade);
		detail.setScore(score);
		detail.setReason(reason);
		return detail;
	}

	private long countUnmatched(List<String> parsedCodes, Set<Long> candidateIds) {
		LambdaQueryWrapper<ExpertEntity> wrapper = Wrappers.<ExpertEntity>lambdaQuery();
		if (!parsedCodes.isEmpty()) {
			wrapper.and(w -> w.isNull(ExpertEntity::getDomainCode).or().notIn(ExpertEntity::getDomainCode, parsedCodes));
		}
		wrapper.notIn(!candidateIds.isEmpty(), ExpertEntity::getId, candidateIds);
		return expertService.count(wrapper);
	}

	private void saveFailedRecord(String query, Exception e, int costMs) {
		try {
			ExtractRecordEntity record = new ExtractRecordEntity();
			record.setQueryText(query);
			record.setStatus("1");
			record.setFailReason(StrUtil.subPre(e.getMessage(), 490));
			record.setCostMs(costMs);
			extractRecordService.save(record);
		}
		catch (Exception ex) {
			log.error("失败抽取记录落库异常", ex);
		}
	}
}
