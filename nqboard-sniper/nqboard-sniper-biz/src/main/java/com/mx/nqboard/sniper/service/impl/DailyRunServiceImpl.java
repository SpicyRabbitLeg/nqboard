package com.mx.nqboard.sniper.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.api.enums.RunStatusEnum;
import com.mx.nqboard.sniper.mapper.DailyRunMapper;
import com.mx.nqboard.sniper.service.DailyRunService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 每日运行记账 服务实现类（含 Quartz/手动任务共用的幂等包装模板）：
 * </p>
 * <ul>
 * <li>UK(run_date, phase) 即幂等键——重复触发按已有行决定：<b>running 态跳过</b>（防并发双跑），
 * done/failed 允许幂等重跑覆盖</li>
 * <li>收尾写 status/detail/budget_used/finished_at；预算消耗（东财主源日预算）由 work 的 detail
 * 携带 {@code budgetUsed} 键写入列</li>
 * <li>本表为业务表（继承 BaseEntity）：走标准 MP save/updateById，雪花 id 与审计五列由填充器自动写</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class DailyRunServiceImpl extends ServiceImpl<DailyRunMapper, DailyRunEntity> implements DailyRunService {

	@Override
	public Map<String, Object> runGuarded(LocalDate runDate, RunPhaseEnum phase, Supplier<Map<String, Object>> work) {
		DailyRunEntity existing = getByRunDateAndPhase(runDate, phase);
		if (existing != null && existing.getStatus() == RunStatusEnum.RUNNING) {
			log.warn("daily run {}/{} is RUNNING; skip concurrent trigger", runDate, phase);
			return Map.of("skipped", true, "reason", "already_running");
		}
		DailyRunEntity run = existing != null ? existing : new DailyRunEntity();
		run.setRunDate(runDate);
		run.setPhase(phase);
		run.setStatus(RunStatusEnum.RUNNING);
		run.setStartedAt(LocalDateTime.now());
		saveOrUpdate(run);

		try {
			// work 只能调一次：三元里重复 get() 会把整个任务流程执行两遍（2026-10-04 实测 Tushare 调用翻倍）
			Map<String, Object> result = work.get();
			Map<String, Object> detail = result != null ? result : new LinkedHashMap<>();
			run.setStatus(RunStatusEnum.DONE);
			run.setDetail(toJson(detail));
			Object budget = detail.get("budgetUsed");
			if (budget instanceof Number number) {
				run.setBudgetUsed(number.intValue());
			}
			run.setFinishedAt(LocalDateTime.now());
			updateById(run);
			log.info("daily run {}/{} done: {}", runDate, phase, detail);
			return detail;
		}
		catch (RuntimeException e) {
			run.setStatus(RunStatusEnum.FAILED);
			Map<String, Object> errorDetail = new LinkedHashMap<>();
			errorDetail.put("error", e.getMessage() == null ? e.toString() : e.getMessage());
			run.setDetail(toJson(errorDetail));
			run.setFinishedAt(LocalDateTime.now());
			updateById(run);
			log.error("daily run {}/{} failed", runDate, phase, e);
			// 任务失败向上抛出由 Job 层记日志；扫描依赖的数据缺失语义由各阶段自身兜底
			throw e;
		}
	}

	private static String toJson(Object value) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
		}
		catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			return "{}";
		}
	}

	private DailyRunEntity getByRunDateAndPhase(LocalDate runDate, RunPhaseEnum phase) {
		return getOne(Wrappers.<DailyRunEntity>lambdaQuery()
			.eq(DailyRunEntity::getRunDate, runDate)
			.eq(DailyRunEntity::getPhase, phase)
			.last("LIMIT 1"), false);
	}

}
