package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;

/**
 * <p>
 * Tushare top_list 龙虎榜每日明细 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockTopListService extends IService<StockTopListEntity> {

	/**
	 * 从 tushare 同步龙虎榜每日明细（全量/增量取 yml 配置 tushare.daily.full）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockTopListService
	 *   methodName   = syncFromTushare
	 *   cronExpression = 0 30 18 * * ?   （示例：每天 18:30 收盘后增量同步）
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromTushare();

	/**
	 * 从 tushare 同步龙虎榜每日明细
	 * <p>
	 * top_list 接口按交易日期查询（一次返回当天全部上榜股票），故按日期遍历：
	 * tradeDate 非空则仅同步该日期；为空时按 full 决定范围（全量=2026-01-01 至今天，跳过周末；增量=仅今天）。
	 * 按唯一键 (trade_date, ts_code) 批量插入/更新。请求间隔因限频已在实现类中写死。
	 * @param tradeDate 指定交易日期 YYYYMMDD，可空
	 * @param full 是否全量同步：true=2026-01-01 至今天；false=仅增量获取今天；为空取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	int syncFromTushare(String tradeDate, Boolean full);

}
