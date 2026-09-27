package com.mx.nqboard.export.service.impl;

import cn.hutool.json.JSONUtil;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordDetailEntity;
import com.mx.nqboard.export.dify.DifyClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExtractRunServiceImpl 纯逻辑单测：分档边界、LLM 复核输出防御解析、批次失败兜底（不依赖 DB/Dify）
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/26
 */
class ExtractRunServiceTest {

	private static ExtractRecordDetailEntity detail(long expertId, String grade, BigDecimal score, String reason) {
		ExtractRecordDetailEntity d = new ExtractRecordDetailEntity();
		d.setExpertId(expertId);
		d.setGrade(grade);
		d.setScore(score);
		d.setReason(reason);
		return d;
	}

	@Test
	@DisplayName("分档：score 等于阈值视为高分直过（≥ 语义），理由追加向量高分")
	void splitReviewBand_boundaryAtThreshold() {
		List<ExtractRecordDetailEntity> pool = new ArrayList<>();
		pool.add(detail(1, "1", new BigDecimal("0.45"), "领域精确命中"));
		List<ExtractRecordDetailEntity> band = ExtractRunServiceImpl.splitReviewBand(pool, new BigDecimal("0.45"));
		assertThat(band).isEmpty();
		assertThat(pool.get(0).getReason()).isEqualTo("领域精确命中，向量高分");
	}

	@Test
	@DisplayName("分档：低于阈值与无分（低于检索底线）进复核带且理由不变")
	void splitReviewBand_lowAndNull() {
		List<ExtractRecordDetailEntity> pool = new ArrayList<>();
		pool.add(detail(1, "1", new BigDecimal("0.4499"), "领域精确命中"));
		pool.add(detail(2, "2", null, "邻接领域命中"));
		pool.add(detail(3, "2", new BigDecimal("0.50"), "向量语义召回"));
		List<ExtractRecordDetailEntity> band = ExtractRunServiceImpl.splitReviewBand(pool, new BigDecimal("0.45"));
		assertThat(band).extracting(ExtractRecordDetailEntity::getExpertId).containsExactly(1L, 2L);
		assertThat(pool.get(0).getReason()).isEqualTo("领域精确命中");
		assertThat(pool.get(2).getReason()).isEqualTo("向量语义召回，向量高分");
	}

	@Test
	@DisplayName("复核解析：relevant=false 判剔除、true 追加通过、index 非法或缺失丢弃、relevant 缺省视为保留")
	void applyReviewResult_defensive() {
		List<ExtractRecordDetailEntity> batch = new ArrayList<>();
		batch.add(detail(1, "2", new BigDecimal("0.40"), "邻接领域命中"));
		batch.add(detail(2, "1", new BigDecimal("0.30"), "领域精确命中"));
		batch.add(detail(3, "2", null, "邻接领域命中"));
		String json = """
				[
					{"index":1,"relevant":false,"reason":"法学与查询意图无关"},
					{"index":2,"relevant":true,"reason":"方向相关"},
					{"index":0,"relevant":false,"reason":"越界下限"},
					{"index":4,"relevant":false,"reason":"越界上限"},
					{"index":3},
					{"relevant":false,"reason":"缺index"}
				]
				""";
		ExtractRunServiceImpl.applyReviewResult(batch, JSONUtil.parseArray(json));
		assertThat(batch.get(0).getGrade()).isEqualTo("3");
		assertThat(batch.get(0).getReason()).isEqualTo("LLM复核剔除：法学与查询意图无关");
		assertThat(batch.get(1).getGrade()).isEqualTo("1");
		assertThat(batch.get(1).getReason()).isEqualTo("领域精确命中，LLM复核通过");
		assertThat(batch.get(2).getGrade()).isEqualTo("2");
		assertThat(batch.get(2).getReason()).isEqualTo("邻接领域命中，LLM复核通过");
	}

	@Test
	@DisplayName("复核剔除理由超长时截断到 255")
	void applyReviewResult_reasonTruncated() {
		List<ExtractRecordDetailEntity> batch = new ArrayList<>();
		batch.add(detail(1, "2", new BigDecimal("0.40"), "邻接领域命中"));
		String longReason = "长".repeat(300);
		ExtractRunServiceImpl.applyReviewResult(batch, JSONUtil.parseArray(
				"[{\"index\":1,\"relevant\":false,\"reason\":\"" + longReason + "\"}]"));
		assertThat(batch.get(0).getReason()).startsWith("LLM复核剔除：");
		assertThat(batch.get(0).getReason().length()).isLessThanOrEqualTo(255);
	}

	@Test
	@DisplayName("探针构建：短查询保持单探针原口径（query+keywords）")
	void buildSearchProbes_shortQuery() {
		List<String> probes = ExtractRunServiceImpl.buildSearchProbes("帮我找Java开发相关专家", List.of("Java", "软件开发"));
		assertThat(probes).containsExactly("帮我找Java开发相关专家 Java 软件开发");
	}

	@Test
	@DisplayName("探针构建：长清单切分多探针、每个不超 250、方向词全覆盖、关键词单独成探针")
	void buildSearchProbes_longQuery() {
		String query = "帮我找" + "有机化学、高等数学、神经生物学、".repeat(40) + "等方向的专家";
		List<String> probes = ExtractRunServiceImpl.buildSearchProbes(query, List.of("化学", "数学"));
		assertThat(probes.size()).isGreaterThan(1);
		assertThat(probes).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(250));
		// 关键词单独成探针（最后一个）
		assertThat(probes.get(probes.size() - 1)).isEqualTo("化学 数学");
		// 除关键词探针外的所有探针拼接后覆盖 query 的每个方向词
		String joined = String.join("|", probes.subList(0, probes.size() - 1));
		for (String token : query.split("[、，,；;。\\s]+")) {
			if (!token.isBlank()) {
				assertThat(joined).contains(token.trim());
			}
		}
	}

	@Test
	@DisplayName("探针构建：无分隔符超长查询按字符硬切且可无损还原")
	void buildSearchProbes_noDelimiter() {
		String query = "长".repeat(600);
		List<String> probes = ExtractRunServiceImpl.buildSearchProbes(query, List.of());
		assertThat(probes).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(250));
		assertThat(String.join("", probes)).isEqualTo(query);
	}

	@Test
	@DisplayName("复核补审：首轮回输出未覆盖的成员自动追加一轮复核")
	void reviewBatch_retriesUncovered() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.eq("review"), Mockito.anyString()))
			.thenReturn("[{\"index\":1,\"relevant\":true}]",
					"[{\"index\":1,\"relevant\":false,\"reason\":\"方向无关\"}]");
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));
		band.add(detail(2, "2", null, "邻接领域命中"));

		service.reviewBatch("找有机化学专家", band, Map.of());

		assertThat(band.get(0).getReason()).isEqualTo("领域精确命中，LLM复核通过");
		assertThat(band.get(1).getGrade()).isEqualTo("3");
		assertThat(band.get(1).getReason()).isEqualTo("LLM复核剔除：方向无关");
		Mockito.verify(difyClient, Mockito.times(2)).runLLMTask(Mockito.eq("review"), Mockito.anyString());
	}

	@Test
	@DisplayName("复核补审：补审仍未覆盖的成员降档候选并标注'复核未覆盖保留'")
	void reviewBatch_uncoveredAfterRetry() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.eq("review"), Mockito.anyString())).thenReturn("[]", "[]");
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));
		band.add(detail(2, "2", null, "邻接领域命中"));

		service.reviewBatch("找有机化学专家", band, Map.of());

		assertThat(band).allSatisfy(d -> {
			assertThat(d.getGrade()).isEqualTo("2");
			assertThat(d.getReason()).endsWith("，复核未覆盖保留");
		});
		Mockito.verify(difyClient, Mockito.times(2)).runLLMTask(Mockito.eq("review"), Mockito.anyString());
	}

	@Test
	@DisplayName("批次失败兜底：已选人降档候选并标注'复核异常保留'")
	void reviewBatch_fallbackOnFailure() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.anyString(), Mockito.anyString()))
			.thenThrow(new RuntimeException("dify down"));
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));
		band.add(detail(2, "2", null, "邻接领域命中"));

		service.reviewBatch("找Java开发相关专家", band, Map.of());

		assertThat(band).allSatisfy(d -> {
			assertThat(d.getGrade()).isEqualTo("2");
			assertThat(d.getReason()).endsWith("，复核异常保留");
		});
		assertThat(progressOf(service, "progressDone")).isEqualTo(2L);
		assertThat(progressOf(service, "progressTotal")).isEqualTo(2L);
	}

	@Test
	@DisplayName("容错解析：数组被包进 JSON 对象时提取内层 result")
	void reviewBatch_objectWrappedOutput() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.eq("review"), Mockito.anyString()))
			.thenReturn("{\"result\":[{\"index\":1,\"relevant\":false,\"reason\":\"方向无关\"}]}");
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));

		service.reviewBatch("找Java开发相关专家", band, Map.of());

		assertThat(band.get(0).getGrade()).isEqualTo("3");
		assertThat(band.get(0).getReason()).isEqualTo("LLM复核剔除：方向无关");
	}

	@Test
	@DisplayName("输出漂移：首次非 JSON 数组自动重试一次并应用判定")
	void reviewBatch_retriesOnFormatDrift() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.eq("review"), Mockito.anyString()))
			.thenReturn("抱歉，我无法完成该任务", "[{\"index\":1,\"relevant\":true}]");
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));

		service.reviewBatch("找Java开发相关专家", band, Map.of());

		assertThat(band.get(0).getGrade()).isEqualTo("1");
		assertThat(band.get(0).getReason()).isEqualTo("领域精确命中，LLM复核通过");
		Mockito.verify(difyClient, Mockito.times(2)).runLLMTask(Mockito.eq("review"), Mockito.anyString());
	}

	private ExtractRunServiceImpl newService(DifyClient difyClient) {
		ExtractRunServiceImpl service = new ExtractRunServiceImpl(difyClient, null, null, null, null, null);
		ReflectionTestUtils.setField(service, "reviewBatchSize", 30);
		return service;
	}

	@Test
	@DisplayName("复核正常流：Dify 返回结果被应用并推进进度")
	void reviewBatch_appliesResult() {
		DifyClient difyClient = Mockito.mock(DifyClient.class);
		Mockito.when(difyClient.runLLMTask(Mockito.eq("review"), Mockito.anyString()))
			.thenReturn("[{\"index\":1,\"relevant\":false,\"reason\":\"方向无关\"},{\"index\":2,\"relevant\":true}]");
		ExtractRunServiceImpl service = newService(difyClient);
		List<ExtractRecordDetailEntity> band = new ArrayList<>();
		band.add(detail(1, "1", new BigDecimal("0.30"), "领域精确命中"));
		band.add(detail(2, "2", null, "邻接领域命中"));

		service.reviewBatch("找Java开发相关专家", band, Map.of());

		assertThat(band.get(0).getGrade()).isEqualTo("3");
		assertThat(band.get(0).getReason()).isEqualTo("LLM复核剔除：方向无关");
		assertThat(band.get(1).getGrade()).isEqualTo("2");
		assertThat(band.get(1).getReason()).isEqualTo("邻接领域命中，LLM复核通过");
		assertThat(progressOf(service, "progressDone")).isEqualTo(2L);
	}

	private static long progressOf(ExtractRunServiceImpl service, String field) {
		return ((java.util.concurrent.atomic.AtomicLong) ReflectionTestUtils.getField(service, field)).get();
	}

}
