package com.mx.nqboard.sniper.data;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 东财主源日预算计数器（照 Python scan/rate_budget.py 语义，主文档 §4.2）：
 * </p>
 * <ul>
 * <li>上限默认 300 次/日；<b>仅"东财主源"模式计数</b>（Tushare 主源不消耗）——模式判断在
 * CompositeProvider 路由层（T7），本组件只管计数与判断</li>
 * <li>全市场快照请求<b>无条件消耗 1 次</b>（B.1 #1，universe.py:113），调用方照常 tryConsume</li>
 * <li>耗尽语义（Stage1 跳票、truncated_at_stage）属管线层（M2），由调用方消费 {@link #exhausted()}</li>
 * <li>跨日自动归零；持久化（sniper_daily_run.budget_used）由 Job 包装器在任务收尾写入（T11），
 * 本组件为内存态</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class DailyBudget {

	private final int limit;

	private final java.util.function.Supplier<LocalDate> today;

	private final AtomicInteger used = new AtomicInteger();

	private final AtomicReference<LocalDate> currentDay = new AtomicReference<>();

	public DailyBudget(int limit) {
		this(limit, LocalDate::now);
	}

	/** today 供测试注入翻转日期 */
	public DailyBudget(int limit, java.util.function.Supplier<LocalDate> today) {
		this.limit = limit;
		this.today = today;
	}

	/**
	 * 尝试消耗 n 次（快照无条件计 1、Stage1 每票计 1 的语义由调用方换算）。
	 * @return false = 预算不足，调用方应跳过并记录（耗尽标记由管线层写入报告 meta）
	 */
	public synchronized boolean tryConsume(int n) {
		rollDayIfNeeded();
		int current = used.get();
		if (current + n > limit) {
			log.debug("daily budget exhausted: used={} + {} > limit={}", current, n, limit);
			return false;
		}
		used.addAndGet(n);
		return true;
	}

	public synchronized int used() {
		rollDayIfNeeded();
		return used.get();
	}

	public synchronized int remaining() {
		rollDayIfNeeded();
		return Math.max(limit - used.get(), 0);
	}

	public synchronized boolean exhausted() {
		rollDayIfNeeded();
		return used.get() >= limit;
	}

	private void rollDayIfNeeded() {
		LocalDate day = today.get();
		if (!day.equals(currentDay.get())) {
			currentDay.set(day);
			used.set(0);
			log.info("daily budget rolled to {}: limit={}", day, limit);
		}
	}

}
