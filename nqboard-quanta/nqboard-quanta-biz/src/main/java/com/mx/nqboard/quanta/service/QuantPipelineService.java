package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.QuantPipelineLogEntity;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 盘后数据流水线 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface QuantPipelineService extends IService<QuantPipelineLogEntity> {

	/**
	 * 同步执行完整盘后流水线（数据同步各步骤按序执行，逐步骤落日志）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = quantPipelineService
	 *   methodName   = runPipeline
	 *   cronExpression = 0 0 16 * * ?
	 * </pre>
	 * <p>
	 * 步骤失败不中断流水线（后续步骤继续），最终状态以 quant_pipeline_log 为准。
	 * 并发保护：同一时刻仅允许一个流水线实例运行。
	 * @return 本次运行的 runId
	 */
	String runPipeline();

	/**
	 * 异步执行完整流水线（立即返回 runId，执行进度查看 quant_pipeline_log）
	 * @return 本次运行的 runId
	 */
	String runPipelineAsync();

	/**
	 * 重跑单个步骤（异步执行）
	 * @param step 步骤编码，如 stock_daily / money_flow
	 * @return 本次运行的 runId
	 */
	String runStepAsync(String step);

	/**
	 * 查询流水线支持的全部步骤定义
	 * @return [{step, stepName}]
	 */
	List<Map<String, String>> listSteps();

	/**
	 * 数据就绪检查：以指数日线最新交易日为基准，校验股票日线当日覆盖率
	 * <p>
	 * 用于筛选/分析等下游步骤执行前判断数据是否同步完成
	 * @return {tradeDate, basicCount, dailyCount, coverage, ready}
	 */
	Map<String, Object> checkReadiness();

	/**
	 * 查询指定日期流水线各步骤的最新执行状态
	 * @param runDate 运行日期 YYYYMMDD
	 * @return 步骤日志列表
	 */
	List<QuantPipelineLogEntity> latestRuns(String runDate);

}
