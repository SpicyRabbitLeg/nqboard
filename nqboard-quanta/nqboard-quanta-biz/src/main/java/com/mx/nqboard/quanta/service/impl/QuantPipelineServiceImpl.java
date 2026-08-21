package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.QuantPipelineLogEntity;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.mapper.QuantPipelineLogMapper;
import com.mx.nqboard.quanta.mapper.StockBasicMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.service.QuantPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * 盘后数据流水线 服务实现类
 * </p>
 * <p>
 * 盘后信号流水线：只编排信号计算链路（筛选打分 -> 候选池 -> LLM分析 -> 持仓跟踪 ->
 * 命中率追踪），逐步骤写 quant_pipeline_log（状态/耗时/影响行数/异常），单步失败不中断。
 * </p>
 * <p>
 * 数据同步（指数/日线/龙虎榜/增减持/股东户数/资金流/板块/解禁）由各自独立的
 * Quartz 定时任务错峰执行（见 nqboard-quartz StockBasicTask），本流水线不包含。
 * 筛选步骤自带数据就绪检查（日线覆盖率），数据未就绪时该步骤置 FAILED 并给出明确原因。
 * 公告新闻同步为逐股票接口，同样由独立定时任务执行。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
public class QuantPipelineServiceImpl extends ServiceImpl<QuantPipelineLogMapper, QuantPipelineLogEntity>
		implements QuantPipelineService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 大盘基准指数（用于数据就绪检查）
	 */
	private static final String BENCHMARK_INDEX = "sh000300";

	/**
	 * 步骤编码：筛选打分（Stage 0 粗滤 + 硬门 + 模板打分）
	 */
	public static final String STEP_SCREEN = "screen";

	/**
	 * 步骤编码：候选池刷新（Gate 硬门 + 过期管理）
	 */
	public static final String STEP_CANDIDATE = "candidate";

	/**
	 * 步骤编码：模拟持仓跟踪（买入成交/离场评估/逐日盯市）
	 */
	public static final String STEP_POSITION_TRACK = "position_track";

	/**
	 * 步骤编码：LLM 分析（Dify Workflow，失败自动规则降级）
	 */
	public static final String STEP_AGENT_ANALYSIS = "agent_analysis";

	/**
	 * 步骤编码：命中率追踪（候选池信号前向收益，试运行观察）
	 */
	public static final String STEP_HIT_RATE = "hit_rate";

	/**
	 * 流水线并发保护：同一时刻仅允许一个实例运行
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * 异步执行线程池（单线程，流水线整体串行）
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "quant-pipeline");
		t.setDaemon(true);
		return t;
	});

	/**
	 * 步骤注册表（LinkedHashMap 保证顺序）
	 */
	private final Map<String, StepDef> steps = new LinkedHashMap<>();

	private final StockIndexDailyMapper stockIndexDailyMapper;

	private final StockDailyMapper stockDailyMapper;

	private final StockBasicMapper stockBasicMapper;

	/**
	 * 数据就绪检查：股票日线当日最低覆盖率（yml 配置 quanta.pipeline.readiness-min-coverage）
	 */
	@Value("${quanta.pipeline.readiness-min-coverage:0.95}")
	private double readinessMinCoverage;

	public QuantPipelineServiceImpl(StockIndexDailyMapper stockIndexDailyMapper,
			StockDailyMapper stockDailyMapper, StockBasicMapper stockBasicMapper,
			com.mx.nqboard.quanta.service.StockScreenResultService stockScreenResultService,
			com.mx.nqboard.quanta.service.StockCandidateService stockCandidateService,
			com.mx.nqboard.quanta.service.StockAgentAnalysisService stockAgentAnalysisService,
			com.mx.nqboard.quanta.service.StockSimPositionService stockSimPositionService,
			com.mx.nqboard.quanta.service.StockCandidateHitService stockCandidateHitService) {
		this.stockIndexDailyMapper = stockIndexDailyMapper;
		this.stockDailyMapper = stockDailyMapper;
		this.stockBasicMapper = stockBasicMapper;
		// 顺序即执行顺序：数据同步由外部定时任务错峰完成后触发本流水线
		register(STEP_SCREEN, "筛选打分", stockScreenResultService::screen);
		register(STEP_CANDIDATE, "候选池刷新", stockCandidateService::refreshCandidates);
		register(STEP_AGENT_ANALYSIS, "LLM分析", stockAgentAnalysisService::analyze);
		register(STEP_POSITION_TRACK, "持仓跟踪", stockSimPositionService::trackPositions);
		register(STEP_HIT_RATE, "命中率追踪", stockCandidateHitService::refreshHits);
	}

	private void register(String step, String stepName, StepAction action) {
		steps.put(step, new StepDef(step, stepName, action));
	}

	@Override
	public String runPipeline() {
		String runId = newRunId();
		if (!running.compareAndSet(false, true)) {
			throw new IllegalStateException("流水线正在运行中，请稍后再试（可通过 quant_pipeline_log 查看进度）");
		}
		try {
			log.info("盘后流水线开始, runId={}, steps={}", runId, steps.keySet());
			for (StepDef step : steps.values()) {
				executeStep(runId, step);
			}
			log.info("盘后流水线结束, runId={}", runId);
			return runId;
		}
		finally {
			running.set(false);
		}
	}

	@Override
	public String runPipelineAsync() {
		String runId = newRunId();
		executor.submit(this::runPipeline);
		return runId;
	}

	@Override
	public String runStepAsync(String step) {
		StepDef def = steps.get(step);
		if (def == null) {
			throw new IllegalArgumentException("未知流水线步骤: " + step + "，可选: " + steps.keySet());
		}
		String runId = newRunId();
		executor.submit(() -> {
			if (!running.compareAndSet(false, true)) {
				log.warn("流水线正在运行中，单步重跑 {} 被拒绝", step);
				return;
			}
			try {
				executeStep(runId, def);
			}
			finally {
				running.set(false);
			}
		});
		return runId;
	}

	@Override
	public List<Map<String, String>> listSteps() {
		List<Map<String, String>> list = new ArrayList<>();
		steps.forEach((code, def) -> list.add(Map.of("step", code, "stepName", def.stepName())));
		return list;
	}

	@Override
	public Map<String, Object> checkReadiness() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		Map<String, Object> result = new LinkedHashMap<>(8);
		if (latest == null || StrUtil.isBlank(latest.getTradeDate())) {
			result.put("ready", false);
			result.put("message", "指数日线无数据，请先执行 index_daily 步骤");
			return result;
		}
		String tradeDate = latest.getTradeDate();
		long basicCount = stockBasicMapper.selectCount(Wrappers.emptyWrapper());
		long dailyCount = stockDailyMapper.selectCount(Wrappers.<StockDailyEntity>lambdaQuery()
				.eq(StockDailyEntity::getTradeDate, tradeDate));
		double coverage = basicCount > 0 ? (double) dailyCount / basicCount : 0.0;
		boolean ready = basicCount > 0 && coverage >= readinessMinCoverage;
		result.put("tradeDate", tradeDate);
		result.put("basicCount", basicCount);
		result.put("dailyCount", dailyCount);
		result.put("coverage", Math.round(coverage * 10000) / 10000.0);
		result.put("ready", ready);
		result.put("minCoverage", readinessMinCoverage);
		return result;
	}

	@Override
	public List<QuantPipelineLogEntity> latestRuns(String runDate) {
		String date = StrUtil.blankToDefault(runDate, LocalDate.now().format(BASIC_DATE));
		List<QuantPipelineLogEntity> logs = list(Wrappers.<QuantPipelineLogEntity>lambdaQuery()
				.eq(QuantPipelineLogEntity::getRunDate, date)
				.orderByDesc(QuantPipelineLogEntity::getId));
		// 同一步骤同日多次执行时仅保留最新一条
		Map<String, QuantPipelineLogEntity> latest = new LinkedHashMap<>();
		for (QuantPipelineLogEntity logEntry : logs) {
			latest.putIfAbsent(logEntry.getStep(), logEntry);
		}
		return latest.values().stream()
				.sorted(Comparator.comparing(l -> stepOrder(l.getStep())))
				.toList();
	}

	private int stepOrder(String step) {
		int i = 0;
		for (String key : steps.keySet()) {
			if (key.equals(step)) {
				return i;
			}
			i++;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * 执行单个步骤并落日志（RUNNING -> SUCCESS/FAILED）
	 */
	private void executeStep(String runId, StepDef step) {
		LocalDateTime begin = LocalDateTime.now();
		QuantPipelineLogEntity logEntry = new QuantPipelineLogEntity();
		logEntry.setRunId(runId);
		logEntry.setRunDate(LocalDate.now().format(BASIC_DATE));
		logEntry.setStep(step.step());
		logEntry.setStepName(step.stepName());
		logEntry.setStatus("RUNNING");
		logEntry.setBeginTime(begin);
		save(logEntry);
		try {
			Integer affected = step.action().run();
			LocalDateTime end = LocalDateTime.now();
			logEntry.setStatus("SUCCESS");
			logEntry.setAffected(affected);
			logEntry.setMessage("同步影响 " + affected + " 行");
			logEntry.setEndTime(end);
			logEntry.setElapsedMs(java.time.Duration.between(begin, end).toMillis());
			updateById(logEntry);
		}
		catch (Exception e) {
			LocalDateTime end = LocalDateTime.now();
			logEntry.setStatus("FAILED");
			logEntry.setException(StrUtil.maxLength(e.toString(), 2000));
			logEntry.setEndTime(end);
			logEntry.setElapsedMs(java.time.Duration.between(begin, end).toMillis());
			updateById(logEntry);
			log.error("流水线步骤 {} 执行失败: {}", step.step(), e.getMessage(), e);
		}
	}

	private String newRunId() {
		return LocalDate.now().format(BASIC_DATE) + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 步骤定义
	 */
	private record StepDef(String step, String stepName, StepAction action) {
	}

	/**
	 * 步骤动作：返回影响行数
	 */
	@FunctionalInterface
	private interface StepAction {

		Integer run() throws Exception;

	}

}
