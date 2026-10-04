package com.mx.nqboard.sniper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * sniper 数据接入配置（主文档 §11 配置树的 data 节最小集；strategy/gate 阈值节随 M2~M3 收编）。
 * 密钥经 Nacos+jasypt 注入，本地默认值仅支撑骨架启动（token 空 → 优雅降级东财主源+预算计数）。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Data
@ConfigurationProperties(prefix = "sniper.data")
public class SniperProperties {

	/** Tushare token（TUSHARE_TOKEN；空 = Tushare 不可用，行情/事件全走东财主源+日预算计数） */
	private String tushareToken = "";

	/** CN_TUSHARE_FIRST：非行情端点也 Tushare 优先（token 非空才生效） */
	private boolean tushareFirst = true;

	/** CN_SCAN_MAX_AKSHARE_REQUESTS：东财主源日预算（快照无条件计 1） */
	private int dailyBudget = 300;

	/** 东财/兜底源节流下限（rate_limit.py max(delay, 0.5)） */
	private long sourceDelayMs = 500;

	/** 东财熔断时长（EM_CIRCUIT_BREAKER_SECONDS） */
	private long emCircuitBreakerSeconds = 300;

	/** 出站请求审计全量记录（Java 默认全量；Python 为 opt-in） */
	private boolean requestLog = true;

}
