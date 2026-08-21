package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;

import java.util.List;

/**
 * <p>
 * 每日筛选打分结果 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockScreenResultService extends IService<StockScreenResultEntity> {

	/**
	 * 执行筛选打分（信号日自动取指数日线最新交易日）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockScreenResultService
	 *   methodName   = screen
	 * </pre>
	 * @return 处理（落库）的股票数
	 */
	int screen();

	/**
	 * 执行筛选打分（指定信号日）
	 * <p>
	 * 流程：数据就绪检查 -> 大盘环境（H8）-> Universe 解析 -> 逐股票
	 * Stage 0 粗滤 -> 特征计算 -> Stage 0.5 硬门（H1-H6）-> 模板匹配打分
	 * -> 按 (trade_date, ts_code) upsert 落库。
	 * 板块共振/主力资金/龙虎榜上下文数据缺失时该项自动降级为 0 分。
	 * @param tradeDate 信号日 YYYYMMDD（打分依据日）
	 * @return 处理（落库）的股票数
	 */
	int screen(String tradeDate);

	/**
	 * 查询指定信号日通过入池线的 TopN 候选（按打分降序）
	 * @param tradeDate 信号日 YYYYMMDD
	 * @param topN 最大数量
	 * @return 候选列表
	 */
	List<StockScreenResultEntity> topCandidates(String tradeDate, int topN);

}
