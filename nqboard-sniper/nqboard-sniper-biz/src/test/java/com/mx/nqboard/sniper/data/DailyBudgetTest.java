package com.mx.nqboard.sniper.data;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DailyBudget 单测——limit 边界、耗尽语义、跨日归零、并发不超限。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class DailyBudgetTest {

	@Test
	@DisplayName("limit 边界：恰好用满成功，超量拒绝")
	void consumesUpToLimit() {
		DailyBudget budget = new DailyBudget(3);

		assertThat(budget.tryConsume(1)).isTrue();
		assertThat(budget.tryConsume(1)).isTrue();
		assertThat(budget.tryConsume(1)).isTrue();
		assertThat(budget.used()).isEqualTo(3);
		assertThat(budget.remaining()).isZero();
		assertThat(budget.exhausted()).isTrue();
		assertThat(budget.tryConsume(1)).as("超限拒绝").isFalse();
	}

	@Test
	@DisplayName("剩余不足整批时整批拒绝（Stage1 跳票语义的前提）")
	void rejectsWhenInsufficientForWholeBatch() {
		DailyBudget budget = new DailyBudget(3);
		assertThat(budget.tryConsume(2)).isTrue();
		// 剩 1 次，消耗 2 次的整批请求拒绝
		assertThat(budget.tryConsume(2)).isFalse();
		assertThat(budget.used()).isEqualTo(2);
		// 消耗 1 次仍可
		assertThat(budget.tryConsume(1)).isTrue();
	}

	@Test
	@DisplayName("跨日自动归零")
	void rollsOnDayChange() {
		AtomicInteger dayOffset = new AtomicInteger();
		LocalDate base = LocalDate.of(2026, 10, 3);
		DailyBudget budget = new DailyBudget(1, () -> base.plusDays(dayOffset.get()));

		assertThat(budget.tryConsume(1)).isTrue();
		assertThat(budget.exhausted()).isTrue();

		dayOffset.incrementAndGet();
		assertThat(budget.used()).as("跨日归零").isZero();
		assertThat(budget.exhausted()).isFalse();
		assertThat(budget.tryConsume(1)).isTrue();
	}

	@Test
	@DisplayName("并发消耗不超限（快照无条件计 1 的场景并发触发）")
	void concurrentConsumesNeverExceedLimit() throws InterruptedException {
		DailyBudget budget = new DailyBudget(100);
		AtomicInteger granted = new AtomicInteger();
		CountDownLatch latch = new CountDownLatch(200);
		for (int i = 0; i < 200; i++) {
			new Thread(() -> {
				try {
					if (budget.tryConsume(1)) {
						granted.incrementAndGet();
					}
				}
				finally {
					latch.countDown();
				}
			}).start();
		}
		latch.await();

		assertThat(granted.get()).isEqualTo(100);
		assertThat(budget.used()).isEqualTo(100);
		assertThat(budget.exhausted()).isTrue();
	}

}
