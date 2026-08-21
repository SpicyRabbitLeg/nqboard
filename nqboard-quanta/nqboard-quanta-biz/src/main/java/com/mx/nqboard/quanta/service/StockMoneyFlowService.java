package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;

/**
 * <p>
 * 个股主力资金流 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockMoneyFlowService extends IService<StockMoneyFlowEntity> {

	/**
	 * 从 东方财富 同步个股主力资金流（当日全市场快照）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockMoneyFlowService
	 *   methodName   = syncFromEastMoney
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromEastMoney();

}
