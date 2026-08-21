package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;

/**
 * <p>
 * 行业板块日线K线 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockIndustryDailyService extends IService<StockIndustryDailyEntity> {

	/**
	 * 从 东方财富 同步行业板块日线K线（全量/增量取 yml 配置）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockIndustryDailyService
	 *   methodName   = syncFromEastMoney
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromEastMoney();

	/**
	 * 从 东方财富 同步行业板块日线K线
	 * <p>
	 * 流程：拉取行业板块列表（clist，fs=m:90+t:2）-> 逐板块调用 push2his kline
	 * 接口（secid=90.BKxxxx）-> 按唯一键 (board_code, trade_date) 批量插入/更新。
	 * 请求间隔与重试因接口限制已在实现类中处理（间隔可由 yml 配置）。
	 * @param full 是否全量同步：true=从2026-01-01起；false=仅今天；为空取 yml 配置
	 * @return 同步成功的条数
	 */
	int syncFromEastMoney(Boolean full);

}
