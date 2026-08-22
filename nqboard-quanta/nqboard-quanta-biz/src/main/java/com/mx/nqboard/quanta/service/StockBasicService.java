package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.dto.SyncResult;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.vo.StockOptionVO;

import java.util.List;

/**
 * <p>
 * Tushare股票基础信息 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockBasicService extends IService<StockBasicEntity> {

	/**
	 * 股票下拉选项分页查询（精简 tsCode/name）
	 * <p>
	 * 前端下拉 remote 搜索 + 滚动到底自动加载下一页；keyword 按代码/名称模糊匹配
	 * @param keyword 关键字，可空（空则查询全部）
	 * @param current 页码，从 1 开始
	 * @param size 每页条数
	 * @return 下拉选项分页结果
	 */
	IPage<StockOptionVO> options(String keyword, long current, long size);

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
	 * @return 同步结果（成功/失败条数，由 @QuantSyncLog 落 quant_sync_log 追溯）
	 */
	SyncResult syncFromTushare(String market);

}
