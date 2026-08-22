package com.mx.nqboard.quanta.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 数据同步日志注解（方法级）
 * </p>
 * <p>
 * 标注在数据同步方法上（如 {@code syncFromTushare}），由
 * {@link QuantSyncLogAspect} 环绕拦截：进入时落 {@code quant_sync_log}
 * RUNNING 行，正常返回（{@code SyncResult}/数值）后回写 SUCCESS 与
 * 成功/失败条数，抛异常回写 FAILED 与截断异常，供前端按时间追溯。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QuantSyncLog {

	/**
	 * 任务编码（quant_sync_log.sync_type），如 stock_daily
	 */
	String type();

	/**
	 * 任务名称（quant_sync_log.sync_name），如 股票日线同步
	 */
	String name();

}
