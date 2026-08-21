package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockMotHolderEntity;

/**
 * <p>
 * Tushare 股东增减持表 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockMotHolderService extends IService<StockMotHolderEntity> {

	/**
	 * 从 tushare 同步股东增减持（市场过滤与全量/增量均取 yml 配置）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockMotHolderService
	 *   methodName   = syncFromTushare
	 *   cronExpression = 0 30 18 * * ?   （示例：每天 18:30 收盘后增量同步）
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromTushare();

	/**
	 * 从 tushare 同步股东增减持
	 * <p>
	 * 流程：读取 stock_basic 全量股票代码（按市场过滤）→ 逐股票调用 tushare stk_holdertrade 接口
	 * → 按唯一键 (ts_code, ann_date, holder_name) 批量插入/更新。
	 * 复用日线同步配置项：tushare.daily.market / tushare.daily.full；
	 * 请求间隔因 stk_holdertrade 限频 100 次/分钟，已在实现类中写死（不随日线配置调整）
	 * @param market 市场类型：主板/创业板/科创板，为空取 yml 配置 tushare.daily.market
	 * @param full 是否全量同步：true=2026-08-01 至今天；false=仅增量获取今天；为空取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	int syncFromTushare(String market, Boolean full);

}
