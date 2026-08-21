package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;

/**
 * <p>
 * 指数日线K线表 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockIndexDailyService extends IService<StockIndexDailyEntity> {

	/**
	 * 从 东方财富 同步指数日线K线（全量/增量取 yml 配置 tushare.daily.full）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockIndexDailyService
	 *   methodName   = syncFromEastMoney
	 *   cronExpression = 0 30 18 * * ?   （示例：每天 18:30 收盘后增量同步）
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromEastMoney();

	/**
	 * 从 东方财富 同步指数日线K线
	 * <p>
	 * 数据来源：push2his kline 接口（klt=101 日线，fqt=0 指数不复权）。
	 * 按 yml 配置的指数列表（index.daily.indexes，如 sh000300）逐个拉取，
	 * 按唯一键 (index_code, trade_date) 批量插入/更新。请求间隔因接口限制已在实现类中写死。
	 * @param full 是否全量同步：true=从2026-08-01起；false=仅今天；为空取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	int syncFromEastMoney(Boolean full);

}
