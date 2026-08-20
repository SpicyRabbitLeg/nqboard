package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;

/**
 * <p>
 * Tushare股票基础信息 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockBasicService extends IService<StockBasicEntity> {

	/**
	 * 从 tushare 同步股票基础信息（按市场）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockBasicService
	 *   methodName   = syncFromTushare
	 *   methodParamsValue = 主板 / 创业板 / 科创板（可留空）
	 *   cronExpression = 0 0 18 * * ?   （示例：每天 18:00）
	 * </pre>
	 * @param market 市场类型：主板/创业板/科创板，为空时同步 "主板"
	 * @return 同步成功的条数
	 */
	int syncFromTushare(String market);

}
