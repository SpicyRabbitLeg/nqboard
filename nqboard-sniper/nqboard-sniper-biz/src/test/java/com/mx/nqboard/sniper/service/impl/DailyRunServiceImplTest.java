package com.mx.nqboard.sniper.service.impl;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.api.enums.RunStatusEnum;
import com.mx.nqboard.sniper.service.DailyRunService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DailyRunServiceImpl.runGuarded 幂等模板单测——running 跳过防并发双跑、done/failed 幂等重跑、
 * failed 原样上抛。MP 的 save/updateById/getOne 用 spy 桩掉（不触库）。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
class DailyRunServiceImplTest {

	@SuppressWarnings("unchecked")
	private final DailyRunServiceImpl service = org.mockito.Mockito.spy(new DailyRunServiceImpl());

	@Test
	@DisplayName("首次执行：写 running → work → done+detail+finishedAt")
	void firstRunWritesRunningThenDone() {
		DailyRunEntity none = null;
		org.mockito.Mockito.doReturn(none).when(service).getOne(any(), eq(false));
		org.mockito.Mockito.doReturn(true).when(service).saveOrUpdate(any(DailyRunEntity.class));
		org.mockito.Mockito.doReturn(true).when(service).updateById(any(DailyRunEntity.class));

		Map<String, Object> detail = service.runGuarded(LocalDate.of(2026, 9, 30), RunPhaseEnum.DATA_UPDATE,
				() -> Map.of("noneRows", 5400, "budgetUsed", 42));

		assertThat(detail).containsEntry("noneRows", 5400);
		// 首插分支：saveOrUpdate（save 写 running）1 次 + updateById（done 收尾）1 次
		verify(service, times(1)).saveOrUpdate(org.mockito.ArgumentMatchers.any(DailyRunEntity.class));
		verify(service, times(1)).updateById(org.mockito.ArgumentCaptor.forClass(DailyRunEntity.class).capture());
	}

	@Test
	@DisplayName("running 态跳过：不执行 work（防并发双跑）")
	void skipsWhenAlreadyRunning() {
		DailyRunEntity running = new DailyRunEntity();
		running.setRunDate(LocalDate.of(2026, 9, 30));
		running.setPhase(RunPhaseEnum.SCAN);
		running.setStatus(RunStatusEnum.RUNNING);
		org.mockito.Mockito.doReturn(running).when(service).getOne(any(), eq(false));

		Map<String, Object> result = service.runGuarded(LocalDate.of(2026, 9, 30), RunPhaseEnum.DATA_UPDATE,
				() -> {
					throw new AssertionError("running 态不应执行 work");
				});

		assertThat(result).containsEntry("skipped", true);
	}

	@Test
	@DisplayName("work 仅执行一次（回归：三元里重复 get() 曾导致全流程跑两遍）")
	void workExecutesExactlyOnce() {
		org.mockito.Mockito.doReturn(null).when(service).getOne(any(), eq(false));
		org.mockito.Mockito.doReturn(true).when(service).saveOrUpdate(any(DailyRunEntity.class));
		org.mockito.Mockito.doReturn(true).when(service).updateById(any(DailyRunEntity.class));
		java.util.concurrent.atomic.AtomicInteger invocations = new java.util.concurrent.atomic.AtomicInteger();

		service.runGuarded(LocalDate.of(2026, 9, 30), RunPhaseEnum.DATA_UPDATE, () -> {
			invocations.incrementAndGet();
			return Map.of("ok", 1);
		});

		assertThat(invocations.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("work 异常：写 failed+error 后原样上抛")
	void failureWritesFailedAndRethrows() {
		org.mockito.Mockito.doReturn(null).when(service).getOne(any(), eq(false));
		org.mockito.Mockito.doReturn(true).when(service).saveOrUpdate(any(DailyRunEntity.class));
		org.mockito.Mockito.doReturn(true).when(service).updateById(any(DailyRunEntity.class));

		assertThatThrownBy(() -> service.runGuarded(LocalDate.of(2026, 9, 30), RunPhaseEnum.DATA_UPDATE,
				() -> {
					throw new IllegalStateException("tushare down");
				})).isInstanceOf(IllegalStateException.class);

		ArgumentCaptor<DailyRunEntity> captor = org.mockito.ArgumentCaptor.forClass(DailyRunEntity.class);
		verify(service, times(1)).updateById(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(RunStatusEnum.FAILED);
		assertThat(captor.getValue().getDetail()).contains("tushare down");
	}

}
