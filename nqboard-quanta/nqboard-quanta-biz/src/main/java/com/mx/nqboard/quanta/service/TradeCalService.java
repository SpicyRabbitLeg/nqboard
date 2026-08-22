package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.dto.SyncResult;
import com.mx.nqboard.quanta.api.entity.TradeCalEntity;

/**
 * 交易日历（tushare trade_cal） 服务类
 *
 * @author SpicyRabbitLeg
 */
public interface TradeCalService extends IService<TradeCalEntity> {

	/**
	 * 从 tushare 同步交易日历（增量区间：当年前后各 prefetch-years 年，按 (exchange, cal_date) upsert）
	 * <p>
	 * 无参重载，便于 Quartz 定时任务直接调用（每日开盘前执行一次，刷新节假日/调休日历）
	 * </p>
	 *
	 * @return 同步结果（成功/失败条数，由 @QuantSyncLog 落 quant_sync_log 追溯）
	 */
	SyncResult syncFromTushare();

	/**
	 * 今天是否开盘（定时任务执行前的交易日闸门）
	 * <p>
	 * 优先查本地日历；当日无数据时按配置自动增量同步一次兜底，仍无数据则退化为周末规则
	 * （周末必休市，工作日按开盘处理，避免同步失败阻断任务）
	 * </p>
	 *
	 * @return true=开盘（交易日），false=休市
	 */
	boolean isOpenToday();

	/**
	 * 指定日期是否为交易日（通用判断，如推算次一交易日）
	 *
	 * @param date 日期 YYYYMMDD
	 * @return true=交易日
	 */
	boolean isTradeDate(String date);

}
