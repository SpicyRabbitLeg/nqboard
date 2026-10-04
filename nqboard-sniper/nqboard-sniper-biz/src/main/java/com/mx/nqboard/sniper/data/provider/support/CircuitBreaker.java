package com.mx.nqboard.sniper.data.provider.support;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * 东财熔断器（照 Python akshare_client 的 EM circuit-breaker 语义）：
 * 7 类连接错误标记 → OPEN 300s（可配），OPEN 期间调用方以单次快速尝试短路，
 * 若仍连接错误则续期熔断。线程安全（monotonic 时钟）。
 * </p>
 * <p>连接错误判定与调用方共同完成：本类只负责 OPEN 状态计时。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public final class CircuitBreaker {

	private final long blockMillis;

	private final ReentrantLock lock = new ReentrantLock();

	private volatile long blockedUntilNanos;

	public CircuitBreaker(long blockSeconds) {
		this.blockMillis = blockSeconds * 1000L;
	}

	public static CircuitBreaker withDefaults() {
		return new CircuitBreaker(300);
	}

	/**
	 * 熔断冷却时长（秒，用于日志）
	 */
	public long blockSeconds() {
		return blockMillis / 1000L;
	}

	/**
	 * 熔断是否 OPEN（OPEN 期间调用方短路为单次快速尝试）
	 */
	public boolean isOpen() {
		return System.nanoTime() < blockedUntilNanos;
	}

	/**
	 * 标记熔断 OPEN（默认 300s 或续期）
	 */
	public void markBlock() {
		markBlock(blockMillis);
	}

	public void markBlock(long millis) {
		lock.lock();
		try {
			long until = System.nanoTime() + millis * 1_000_000L;
			if (until > blockedUntilNanos) {
				blockedUntilNanos = until;
			}
		}
		finally {
			lock.unlock();
		}
	}

}
