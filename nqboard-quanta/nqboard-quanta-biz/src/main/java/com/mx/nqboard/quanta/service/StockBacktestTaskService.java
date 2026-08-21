package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockBacktestTaskEntity;
import com.mx.nqboard.quanta.backtest.BacktestParams;

/**
 * <p>
 * 回测任务 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockBacktestTaskService extends IService<StockBacktestTaskEntity> {

	/**
	 * 创建回测任务并异步执行
	 * @param params 回测参数
	 * @return 任务id
	 */
	Long createTask(BacktestParams params);

	/**
	 * 同步执行指定任务（Quartz 周校准任务可反射调用）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockBacktestTaskService
	 *   methodName   = runLatest
	 * </pre>
	 * @return 执行成功的任务id（无 PENDING/FAILED 任务时创建默认参数任务并执行）
	 */
	Long runLatest();

	/**
	 * 重跑指定任务（删除旧成交明细后重新执行）
	 * @param taskId 任务id
	 * @return 任务id
	 */
	Long rerun(Long taskId);

}
