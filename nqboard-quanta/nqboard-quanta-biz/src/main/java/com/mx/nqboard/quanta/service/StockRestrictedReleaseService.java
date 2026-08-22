package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.dto.SyncResult;
import com.mx.nqboard.quanta.api.entity.StockRestrictedReleaseEntity;

/**
 * <p>
 * 限售解禁 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockRestrictedReleaseService extends IService<StockRestrictedReleaseEntity> {

	/**
	 * 从 tushare 同步限售解禁数据（增量：仅今天公告）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockRestrictedReleaseService
	 *   methodName   = syncFromTushare
	 * </pre>
	 * @return 同步结果（成功/失败条数，由 @QuantSyncLog 落 quant_sync_log 追溯）
	 */
	SyncResult syncFromTushare();

	/**
	 * 从 tushare 同步限售解禁数据（share_float 接口）
	 * <p>
	 * 增量模式按 ann_date=今天 单次拉取；全量模式自 yml 配置的回补起始日
	 * （restricted-release.lookback-days，默认90天）逐个公告日拉取。
	 * 按唯一键 (ts_code, float_date, holder_name) 批量插入/更新。
	 * @param full 是否全量回补：true=按回补天数逐日拉取；false=仅今天；为空取增量
	 * @return 同步结果（成功/失败条数，由 @QuantSyncLog 落 quant_sync_log 追溯）
	 */
	SyncResult syncFromTushare(Boolean full);

}
