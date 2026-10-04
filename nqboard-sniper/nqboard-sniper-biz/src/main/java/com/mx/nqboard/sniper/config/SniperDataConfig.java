package com.mx.nqboard.sniper.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mx.nqboard.sniper.data.DailyBudget;
import com.mx.nqboard.sniper.data.RequestAuditSink;
import com.mx.nqboard.sniper.data.RequestLogSink;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.data.provider.em.EastmoneyClient;
import com.mx.nqboard.sniper.data.provider.sina.SinaClient;
import com.mx.nqboard.sniper.data.provider.support.CircuitBreaker;
import com.mx.nqboard.sniper.data.provider.tencent.TencentClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.mx.nqboard.sniper.service.RequestLogService;

/**
 * <p>
 * 数据接入层 Spring 装配（provider 客户端为纯 Java 类，此处组装 bean 图）：
 * </p>
 * <ul>
 * <li>token 空 → tusharePreferredForPrices=false、tushareFirst=false（照 Python
 * tushare_client.available() 语义）——整链优雅降级到东财主源并启用日预算计数</li>
 * <li>审计：RequestLogSink 绑定 request_log 落表，全部出站请求成功/失败各记一条</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SniperProperties.class)
public class SniperDataConfig {

	@Bean
	public DailyBudget sniperDailyBudget(SniperProperties properties) {
		return new DailyBudget(properties.getDailyBudget());
	}

	@Bean
	public RequestAuditSink sniperRequestAuditSink(SniperProperties properties, RequestLogService requestLogService) {
		return properties.isRequestLog() ? new RequestLogSink(requestLogService) : null;
	}

	@Bean
	public TushareClient sniperTushareClient(SniperProperties properties, RequestAuditSink sniperRequestAuditSink) {
		return new TushareClient(properties.getTushareToken(), sniperRequestAuditSink);
	}

	@Bean
	public EastmoneyClient sniperEastmoneyClient(SniperProperties properties, RequestAuditSink sniperRequestAuditSink) {
		return new EastmoneyClient(
				CircuitBreaker.withDefaults(), sniperRequestAuditSink);
	}

	@Bean
	public TencentClient sniperTencentClient(RequestAuditSink sniperRequestAuditSink) {
		return new TencentClient(sniperRequestAuditSink);
	}

	@Bean
	public SinaClient sniperSinaClient(RequestAuditSink sniperRequestAuditSink) {
		return new SinaClient(sniperRequestAuditSink);
	}

	@Bean
	public CompositeProvider sniperCompositeProvider(SniperProperties properties, TushareClient sniperTushareClient,
			EastmoneyClient sniperEastmoneyClient, TencentClient sniperTencentClient, SinaClient sniperSinaClient,
			DailyBudget sniperDailyBudget) {
		// token 空 = Tushare 不可用 → 双开关关闭，整链走东财主源+预算计数（available() 语义）
		boolean tushareAvailable = properties.getTushareToken() != null
				&& !properties.getTushareToken().isBlank();
		return new CompositeProvider(sniperTushareClient, sniperEastmoneyClient, sniperTencentClient,
				sniperSinaClient, sniperDailyBudget, tushareAvailable && properties.isTushareFirst(),
				tushareAvailable && properties.isTushareFirst(), true);
	}

}
