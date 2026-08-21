package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockSimPositionEntity;

/**
 * <p>
 * 模拟持仓 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockSimPositionService extends IService<StockSimPositionEntity> {

	/**
	 * 从候选创建模拟买入计划委托（信号日次一交易日开盘价成交）
	 * <p>
	 * T+1 真实化：委托创建后处于 PENDING_BUY 状态，次日 trackPositions 时以开盘价成交，
	 * 跳空 >+5%（追高）或 <-3%（隔夜利空）自动放弃（CANCELLED），与回测成交假设完全一致。
	 * @param candidateId 候选记录id（须为最新交易日的 ACTIVE 候选）
	 * @return 持仓记录id
	 */
	Long createPendingBuy(Long candidateId);

	/**
	 * 持仓跟踪（交易日自动取指数日线最新交易日）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockSimPositionService
	 *   methodName   = trackPositions
	 * </pre>
	 * @return 处理的持仓数
	 */
	int trackPositions();

	/**
	 * 持仓跟踪（指定交易日，收盘数据就绪后调用）
	 * <p>
	 * 处理顺序：PENDING_SELL 开盘成交 -> PENDING_BUY 开盘成交（H7跳空检查+仓位计算）
	 * -> HOLDING 开盘急杀检查 -> HOLDING 收盘离场评估（ExitEngine 六规则）
	 * -> 写逐日盯市记录（stock_position_daily）。
	 * @param tradeDate 交易日 YYYYMMDD
	 * @return 处理的持仓数
	 */
	int trackPositions(String tradeDate);

}
