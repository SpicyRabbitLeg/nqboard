package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.QuantSyncLogEntity;

import java.util.List;

/**
 * <p>
 * 数据同步执行日志 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface QuantSyncLogService extends IService<QuantSyncLogEntity> {

	/**
	 * 查询指定日期各同步任务最新执行状态（默认今天）
	 * <p>
	 * 同一任务同日多次执行时仅保留最新一条，按开始时间倒序返回
	 * @param runDate 运行日期 YYYYMMDD，为空取今天
	 * @return 各任务最新日志列表
	 */
	List<QuantSyncLogEntity> latestRuns(String runDate);

}
