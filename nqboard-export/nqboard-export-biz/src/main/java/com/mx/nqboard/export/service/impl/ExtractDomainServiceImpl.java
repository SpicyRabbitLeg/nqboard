package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.common.core.exception.CheckedException;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractDomainEntity;
import com.mx.nqboard.export.dify.DifyClient;
import com.mx.nqboard.export.dify.ExtractPrompts;
import com.mx.nqboard.export.mapper.ExtractDomainMapper;
import com.mx.nqboard.export.service.ExpertService;
import com.mx.nqboard.export.service.ExtractDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家抽取领域管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractDomainServiceImpl extends ServiceImpl<ExtractDomainMapper, ExtractDomainEntity>
		implements ExtractDomainService {

	/**
	 * 打标单批组合数
	 */
	private static final int TAG_BATCH_SIZE = 30;

	/**
	 * 领域提案单批方向数
	 */
	private static final int PROPOSE_BATCH_SIZE = 50;

	private final DifyClient difyClient;

	private final ExpertService expertService;

	/**
	 * 打标任务运行标记（防并发重入）
	 */
	private final AtomicBoolean taggingRunning = new AtomicBoolean(false);

	private final AtomicLong taggingDone = new AtomicLong(0);

	private final AtomicLong taggingTotal = new AtomicLong(0);

	/**
	 * AI 归纳任务运行标记（防并发重入）
	 */
	private final AtomicBoolean generateRunning = new AtomicBoolean(false);

	/**
	 * AI 归纳进度计数：propose 批次 + 1 次 merge
	 */
	private final AtomicLong generateDone = new AtomicLong(0);

	private final AtomicLong generateTotal = new AtomicLong(0);

	/**
	 * AI 归纳失败原因（null 表示无错误）
	 */
	private volatile String generateError;

	/**
	 * AI 归纳完成的草稿结果（保留至下次触发）
	 */
	private volatile List<ExtractDomainEntity> generateResult;

	@Override
	public List<ExtractDomainEntity> listDomains(String status) {
		return this.list(Wrappers.<ExtractDomainEntity>lambdaQuery()
			.eq(StrUtil.isNotBlank(status), ExtractDomainEntity::getStatus, status)
			.orderByAsc(ExtractDomainEntity::getSeq));
	}

	@Override
	public Boolean saveDomain(ExtractDomainEntity domain) {
		checkCodeUnique(domain);
		return this.save(domain);
	}

	@Override
	public Boolean updateDomain(ExtractDomainEntity domain) {
		checkCodeUnique(domain);
		return this.updateById(domain);
	}

	@Override
	public Boolean removeDomains(Long[] ids) {
		return this.removeBatchByIds(CollUtil.toList(ids));
	}

	@Override
	public void startGenerate() {
		List<Map<String, Object>> combos = expertService.listMaps(Wrappers.<ExpertEntity>query()
			.select("DISTINCT subject_category, first_discipline, research_direction")
			.isNotNull("research_direction")
			.ne("research_direction", ""));
		if (CollUtil.isEmpty(combos)) {
			throw new CheckedException("专家研究方向组合为空，无法归纳领域清单");
		}
		if (!generateRunning.compareAndSet(false, true)) {
			throw new CheckedException("AI 归纳任务正在执行中，请稍后再试");
		}
		generateResult = null;
		generateError = null;
		CompletableFuture.runAsync(() -> {
			try {
				doGenerate(combos);
			}
			catch (Exception e) {
				log.error("AI 归纳领域清单任务异常终止", e);
				generateError = StrUtil.blankToDefault(e.getMessage(), "AI 归纳任务异常终止");
			}
			finally {
				generateRunning.set(false);
			}
		});
		log.info("AI 归纳领域清单任务已启动：组合数 {}", combos.size());
	}

	@Override
	public Map<String, Object> getGenerateProgress() {
		Map<String, Object> progress = new LinkedHashMap<>();
		progress.put("running", generateRunning.get());
		progress.put("done", generateDone.get());
		progress.put("total", generateTotal.get());
		progress.put("error", generateError);
		return progress;
	}

	@Override
	public List<ExtractDomainEntity> getGenerateResult() {
		if (generateRunning.get()) {
			throw new CheckedException("AI 归纳任务正在执行中，请稍候");
		}
		if (generateResult == null) {
			throw new CheckedException("暂无归纳结果，请先执行 AI 归纳");
		}
		return generateResult;
	}

	/**
	 * 归纳主流程：分批调 LLM 提案（total = 批次数 + 1 次 merge），合并去重为草稿后暂存内存
	 */
	private void doGenerate(List<Map<String, Object>> combos) {
		// 阶段A：分批归纳候选领域
		List<List<Map<String, Object>>> batches = CollUtil.split(combos, PROPOSE_BATCH_SIZE);
		generateTotal.set(batches.size() + 1);
		generateDone.set(0);
		List<Map<String, Object>> proposals = new ArrayList<>();
		for (List<Map<String, Object>> batch : batches) {
			String result = difyClient.runLLMTask("propose", ExtractPrompts.propose(JSONUtil.toJsonStr(batch)));
			JSONArray array = JSONUtil.parseArray(result);
			for (Object item : array) {
				JSONObject obj = (JSONObject) item;
				if (StrUtil.isNotBlank(obj.getStr("domain_name"))) {
					proposals.add(obj);
				}
			}
			generateDone.incrementAndGet();
		}
		if (CollUtil.isEmpty(proposals)) {
			throw new CheckedException("AI 未归纳出任何领域提案，请检查 Dify 服务后重试");
		}
		// 阶段B：合并去重为最终领域清单
		String merged = difyClient.runLLMTask("merge", ExtractPrompts.merge(JSONUtil.toJsonStr(proposals)));
		JSONArray array = JSONUtil.parseArray(merged);
		List<ExtractDomainEntity> draft = new ArrayList<>();
		Map<String, String> nameToCode = new LinkedHashMap<>();
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			ExtractDomainEntity domain = new ExtractDomainEntity();
			String code = StrUtil.blankToDefault(obj.getStr("domain_code"), String.format("D%02d", i + 1));
			domain.setDomainCode(code);
			domain.setDomainName(obj.getStr("domain_name"));
			domain.setDescription(obj.getStr("description"));
			domain.setKeywords(obj.getStr("keywords"));
			domain.setStatus("0");
			domain.setSeq(i + 1);
			draft.add(domain);
			nameToCode.put(domain.getDomainName(), code);
		}
		// 邻接领域名称转编码
		for (int i = 0; i < array.size(); i++) {
			JSONArray adjacent = array.getJSONObject(i).getJSONArray("adjacent");
			if (adjacent == null) {
				continue;
			}
			List<String> codes = adjacent.stream()
				.map(String::valueOf)
				.map(nameToCode::get)
				.filter(Objects::nonNull)
				.toList();
			if (CollUtil.isNotEmpty(codes)) {
				draft.get(i).setAdjacentCodes(JSONUtil.toJsonStr(codes));
			}
		}
		generateDone.incrementAndGet();
		generateResult = draft;
		log.info("AI 归纳领域清单完成：领域数 {}", draft.size());
	}

	@Override
	public Boolean saveBatchDomains(List<ExtractDomainEntity> domains) {
		if (CollUtil.isEmpty(domains)) {
			throw new CheckedException("领域草稿为空");
		}
		for (ExtractDomainEntity domain : domains) {
			checkCodeUnique(domain);
		}
		return this.saveBatch(domains);
	}

	@Override
	public void startTagging() {
		List<ExtractDomainEntity> domains = this.list(Wrappers.<ExtractDomainEntity>lambdaQuery()
			.eq(ExtractDomainEntity::getStatus, "0")
			.orderByAsc(ExtractDomainEntity::getSeq));
		if (CollUtil.isEmpty(domains)) {
			throw new CheckedException("领域清单为空，请先维护领域清单");
		}
		if (!taggingRunning.compareAndSet(false, true)) {
			throw new CheckedException("打标任务正在执行中，请稍后再试");
		}
		CompletableFuture.runAsync(() -> {
			try {
				doTagging(domains);
			}
			catch (Exception e) {
				log.error("领域打标任务异常终止", e);
			}
			finally {
				taggingRunning.set(false);
			}
		});
		log.info("领域打标任务已启动");
	}

	@Override
	public Map<String, Object> getTaggingProgress() {
		return Map.of("running", taggingRunning.get(), "done", taggingDone.get(), "total", taggingTotal.get());
	}

	/**
	 * 打标主流程：按 distinct 研究方向组合分批调 LLM 归类，再按组合回填专家领域标签
	 */
	private void doTagging(List<ExtractDomainEntity> domains) {
		String domainListJson = JSONUtil.toJsonStr(domains.stream()
			.map(d -> Map.of("domain_code", d.getDomainCode(), "domain_name", StrUtil.nullToEmpty(d.getDomainName()),
					"description", StrUtil.nullToEmpty(d.getDescription()), "keywords",
					StrUtil.nullToEmpty(d.getKeywords())))
			.toList());
		Map<String, ExtractDomainEntity> byCode = domains.stream()
			.collect(Collectors.toMap(ExtractDomainEntity::getDomainCode, Function.identity(), (a, b) -> a));

		// 全量专家按 研究方向组合 分组（只取回填所需字段，走主键批量更新）
		List<ExpertEntity> experts = expertService
			.list(Wrappers.<ExpertEntity>lambdaQuery()
				.select(ExpertEntity::getId, ExpertEntity::getSubjectCategory, ExpertEntity::getFirstDiscipline,
						ExpertEntity::getResearchDirection)
				.isNotNull(ExpertEntity::getResearchDirection)
				.ne(ExpertEntity::getResearchDirection, ""));
		Map<String, List<Long>> expertIdsByKey = new LinkedHashMap<>();
		Map<String, Map<String, Object>> comboByKey = new LinkedHashMap<>();
		for (ExpertEntity expert : experts) {
			String key = comboKey(expert.getSubjectCategory(), expert.getFirstDiscipline(), expert.getResearchDirection());
			expertIdsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(expert.getId());
			comboByKey.computeIfAbsent(key, k -> comboMeta(expert));
		}
		List<Map<String, Object>> combos = new ArrayList<>(comboByKey.values());
		taggingTotal.set(combos.size());
		taggingDone.set(0);
		log.info("领域打标开始：组合数 {}，专家数 {}", combos.size(), experts.size());

		List<ExpertEntity> updates = new ArrayList<>();
		for (List<Map<String, Object>> batch : CollUtil.split(combos, TAG_BATCH_SIZE)) {
			try {
				List<Map<String, Object>> indexed = new ArrayList<>();
				for (int i = 0; i < batch.size(); i++) {
					indexed.add(Map.of("index", i + 1, "subject_category",
							StrUtil.nullToEmpty((String) batch.get(i).get("subject_category")), "first_discipline",
							StrUtil.nullToEmpty((String) batch.get(i).get("first_discipline")), "research_direction",
							StrUtil.nullToEmpty((String) batch.get(i).get("research_direction"))));
				}
				String result = difyClient.runLLMTask("tag",
						ExtractPrompts.tag(domainListJson, JSONUtil.toJsonStr(indexed)));
				JSONArray array = JSONUtil.parseArray(result);
				for (Object item : array) {
					JSONObject obj = (JSONObject) item;
					Integer index = obj.getInt("index");
					String code = obj.getStr("domain_code");
					if (index == null || index < 1 || index > batch.size() || StrUtil.isBlank(code)) {
						continue;
					}
					ExtractDomainEntity domain = byCode.get(code);
					if (domain == null) {
						continue;
					}
					String key = comboKey((String) batch.get(index - 1).get("subject_category"),
							(String) batch.get(index - 1).get("first_discipline"),
							(String) batch.get(index - 1).get("research_direction"));
					for (Long expertId : expertIdsByKey.getOrDefault(key, Collections.emptyList())) {
						ExpertEntity update = new ExpertEntity();
						update.setId(expertId);
						update.setDomainCode(domain.getDomainCode());
						update.setDomainName(domain.getDomainName());
						updates.add(update);
					}
				}
			}
			catch (Exception e) {
				log.error("领域打标批次失败，跳过 {} 条组合", batch.size(), e);
			}
			taggingDone.addAndGet(batch.size());
			// 分批回填，避免大集合常驻内存
			if (updates.size() >= 5000) {
				expertService.updateBatchById(updates);
				updates.clear();
			}
		}
		if (CollUtil.isNotEmpty(updates)) {
			expertService.updateBatchById(updates);
		}
		log.info("领域打标完成：处理组合 {}/{}", taggingDone.get(), taggingTotal.get());
	}

	private void checkCodeUnique(ExtractDomainEntity domain) {
		long cnt = this.lambdaQuery()
			.eq(ExtractDomainEntity::getDomainCode, domain.getDomainCode())
			.ne(domain.getId() != null, ExtractDomainEntity::getId, domain.getId())
			.count();
		if (cnt > 0) {
			throw new CheckedException("领域编码已存在: " + domain.getDomainCode());
		}
	}

	private String comboKey(String category, String discipline, String direction) {
		return StrUtil.nullToEmpty(category) + "|" + StrUtil.nullToEmpty(discipline) + "|" + StrUtil.nullToEmpty(direction);
	}

	private Map<String, Object> comboMeta(ExpertEntity expert) {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("subject_category", StrUtil.nullToEmpty(expert.getSubjectCategory()));
		meta.put("first_discipline", StrUtil.nullToEmpty(expert.getFirstDiscipline()));
		meta.put("research_direction", StrUtil.nullToEmpty(expert.getResearchDirection()));
		return meta;
	}
}
