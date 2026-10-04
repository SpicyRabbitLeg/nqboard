package com.mx.nqboard.sniper.data;

/**
 * <p>
 * 出站请求审计钩子（Python log_request 等价物）——所有 provider 客户端每次"逻辑调用"
 * （含重试与翻页合并）成功/失败各回调一次，实现方统一落 {@code sniper_request_log}。
 * </p>
 * <p>语义对齐 {@code src/data/request_log.py}：params 平铺、n_rows 摘要、失败带 error。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@FunctionalInterface
public interface RequestAuditSink {

	/**
	 * 记录一次出站请求（成功与失败都必须落）。
	 * @param endpoint 端点标识，如 tushare.daily / em.kline / tencent.fqkline
	 * @param source 数据源 tushare/em/tencent/sina
	 * @param paramsJson 请求参数 JSON 文本
	 * @param status ok / error
	 * @param nRows 返回行数（失败时 null）
	 * @param elapsedMs 整次逻辑调用耗时（含重试与翻页）
	 * @param error 错误摘要（成功时 null）
	 */
	void accept(String endpoint, String source, String paramsJson, String status, Integer nRows, Long elapsedMs,
			String error);

}
