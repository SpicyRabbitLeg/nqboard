package com.mx.nqboard.quanta.screen;

import lombok.Data;



/**
 * <p>
 * 短线特征向量（由日线K线计算，口径对齐 ai-hedge-fund
 * compute_technical_features(profile=short) + 硬门扩展项）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
public class ScreenFeatures {

	/**
	 * TS股票代码
	 */
	private String tsCode;

	/**
	 * K线数量
	 */
	private int barCount;

	/**
	 * 最新收盘价
	 */
	private double close;

	/**
	 * 最新开盘价
	 */
	private double open;

	/**
	 * 最新最高价
	 */
	private double high;

	/**
	 * 最新最低价
	 */
	private double low;

	/**
	 * 昨收价
	 */
	private double prevClose;

	/**
	 * 当日涨跌幅（%）
	 */
	private double pctChg;

	/**
	 * 当日成交额（元）
	 */
	private double amountYuan;

	/**
	 * 是否一字板（open=high=low=close）
	 */
	private boolean oneWordBoard;

	/**
	 * 是否涨停附近（涨幅 ≥ 板块涨停率-0.3个百分点）
	 */
	private boolean nearLimitUp;

	/**
	 * 距涨停价距离（涨停价-收盘)/收盘
	 */
	private double limitUpDistancePct;

	/**
	 * 连续涨停天数（最多回看5日）
	 */
	private int consecutiveLimitUp;

	/**
	 * EMA5
	 */
	private double ema5;

	/**
	 * EMA10
	 */
	private double ema10;

	/**
	 * EMA20
	 */
	private double ema20;

	/**
	 * EMA20 是否上行（最新值 > 5根K线前）
	 */
	private boolean ema20SlopeUp;

	/**
	 * EMA 排列：1多头 / -1空头 / 0纠缠
	 */
	private int emaAlign;

	/**
	 * 收盘价相对 EMA20 乖离（close/ema20-1）
	 */
	private double emaSpread;

	/**
	 * MACD 柱（(dif-dea)*2）
	 */
	private double macdHist;

	/**
	 * MACD 是否金叉（柱上穿0）
	 */
	private boolean macdGoldCross;

	/**
	 * DIF 归一化（dif/close）
	 */
	private double difNorm;

	/**
	 * RSI6
	 */
	private double rsi6;

	/**
	 * 5日动量
	 */
	private double mom5;

	/**
	 * 20日动量
	 */
	private double mom20;

	/**
	 * 量比（5日均量/20日均量，中期量能趋势）
	 */
	private double volRatio;

	/**
	 * 当日量比（当日成交量/前5日均量，短线爆发力度）
	 */
	private double volRatioToday;

	/**
	 * 20日最高价
	 */
	private double high20;

	/**
	 * 是否突破20日新高（close ≥ high20*0.995）
	 */
	private boolean breakoutUp;

	/**
	 * MA5/MA20 乖离（ma5/ma20-1）
	 */
	private double maRatio;

	/**
	 * 20日年化波动率
	 */
	private double volatility20;

	/**
	 * 当日收盘位置（(close-low)/(high-low)，一字板取1.0）
	 */
	private double closePosition;

	/**
	 * 近60日日均成交额（元）
	 */
	private double avgAmount60Yuan;

	/**
	 * 5日最低价
	 */
	private double low5;

	/**
	 * 20日最低价
	 */
	private double low20;

}
