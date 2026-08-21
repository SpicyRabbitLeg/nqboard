package com.mx.nqboard.quanta.backtest;

import lombok.Data;

/**
 * <p>
 * 回测参数（JSON 序列化存入 stock_backtest_task.params）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
public class BacktestParams {

	/**
	 * 股票池范围：all/hs300_csi500/hs300/csi500
	 */
	private String universe = "hs300_csi500";

	/**
	 * 回测交易日数
	 */
	private int days = 120;

	/**
	 * 入池线（与实盘筛选一致，默认 65）
	 */
	private double minScore = 65;

	/**
	 * 每日候选上限（topN）
	 */
	private int topN = 3;

	/**
	 * 初始资金（元）
	 */
	private double capital = 100_000;

	/**
	 * 最大并发持仓数
	 */
	private int maxPositions = 4;

	/**
	 * 单仓位最大资金占比
	 */
	private double maxPositionPct = 0.25;

	/**
	 * 买入成本率（佣金）
	 */
	private double costBuy = 0.0003;

	/**
	 * 卖出成本率（佣金+印花税）
	 */
	private double costSell = 0.0008;

	/**
	 * 结束日期 YYYYMMDD（默认最新交易日）
	 */
	private String endDate;

}
