package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockConsWeightEntity;

/**
 * <p>
 * 指数成分股及权重 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockConsWeightService extends IService<StockConsWeightEntity> {

	/**
	 * 从 中证指数官网 同步指数成分股权重（下载 yml 配置的全部指数 closeweight.xls）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockConsWeightService
	 *   methodName   = syncFromCsindex
	 *   cronExpression = 0 30 18 * * ?   （示例：每天 18:30 收盘后增量同步）
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromCsindex();

	/**
	 * 从本地 xls 文件同步指数成分股权重（解析后按唯一键批量插入/更新）
	 * <p>
	 * 文件内包含指数代码/名称/日期，无需额外指定；可用于官网文件下载失败时的补录
	 * @param filePath 本地 xls 文件绝对路径
	 * @return 同步成功的条数
	 */
	int syncFromCsindex(String filePath);

}
