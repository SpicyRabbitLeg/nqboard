package com.mx.nqboard.quanta.screen;

/**
 * <p>
 * 短线筛选算法阈值（T+1、3-5日持有期、精度优先）
 * </p>
 * <p>
 * 三层结构：硬性否决门（H1-H10）-> 入场模板（四选一）-> 质量加分。
 * 数值口径移植自 ai-hedge-fund short_metrics.py 并按精度优先策略收紧，
 * 部分阈值可由 yml（quanta.screen.*）覆盖。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public final class ScreenConstants {

	private ScreenConstants() {
	}

	// ==================== Stage 0 粗滤 ====================

	/**
	 * 当日最低成交额（元），默认 1 亿
	 */
	public static final double STAGE0_MIN_AMOUNT_YUAN = 100_000_000d;

	/**
	 * 最低上市天数（过滤次新股）
	 */
	public static final int STAGE0_MIN_LISTING_DAYS = 60;

	/**
	 * 涨停附近剔除：当日涨幅 ≥ 板块涨停率 - 该值（百分点）视为涨停附近
	 */
	public static final double STAGE0_LIMIT_UP_MARGIN_PCT = 0.3;

	// ==================== Stage 0.5 硬性否决门 ====================

	/**
	 * H2：5日最低价须高于 20日最低价 × 该系数（排除下跌中继）
	 */
	public static final double H2_LOW_BUFFER = 1.02;

	/**
	 * H3：20日年化波动率上限
	 */
	public static final double H3_MAX_VOLATILITY = 0.60;

	/**
	 * H4：近60日日均成交额下限（元），默认 8000 万
	 */
	public static final double H4_MIN_AVG_AMOUNT_60D = 80_000_000d;

	/**
	 * H5：巨量滞涨否决——当日量比（当日量/前5日均量）超过该值且涨幅不足该值（%）视为出货嫌疑
	 */
	public static final double H5_VOL_RATIO = 4.0;

	/**
	 * H5：巨量滞涨否决的涨幅上限（百分点）
	 */
	public static final double H5_MAX_PCT_CHG = 2.0;

	/**
	 * H6：当日收盘价须位于当日振幅上半区（收盘位置 ≥ 0.5）
	 */
	public static final double H6_MIN_CLOSE_POSITION = 0.5;

	/**
	 * H7（执行时检查）：次日开盘跳空超过该值放弃买入（T+1 追高保护）
	 */
	public static final double H7_GAP_UP_LIMIT = 0.05;

	/**
	 * H7（执行时检查）：次日开盘跳空低于该值放弃买入（隔夜利空保护）
	 */
	public static final double H7_GAP_DOWN_LIMIT = -0.03;

	/**
	 * H8：大盘门——沪深300 5日收益低于该值当日不产生新候选
	 */
	public static final double H8_MARKET_MIN_RET_5D = -0.03;

	// ==================== 入场模板（四选一） ====================

	/**
	 * 模板A 突破启动：量比下限
	 */
	public static final double BREAKOUT_VOL_RATIO_MIN = 1.5;

	/**
	 * 模板A 突破启动：量比上限（天量警戒）
	 */
	public static final double BREAKOUT_VOL_RATIO_MAX = 3.0;

	/**
	 * 模板A 突破启动：5日动量下限
	 */
	public static final double BREAKOUT_MOM5_MIN = 0.02;

	/**
	 * 模板A 突破启动：5日动量上限
	 */
	public static final double BREAKOUT_MOM5_MAX = 0.08;

	/**
	 * 模板A 突破启动：20日动量上限（首次突破，排除高位二次突破）
	 */
	public static final double BREAKOUT_MOM20_MAX = 0.15;

	/**
	 * 模板B 强势回踩低吸：20日动量下限
	 */
	public static final double PULLBACK_MOM20_MIN = 0.10;

	/**
	 * 模板B 强势回踩低吸：20日动量上限
	 */
	public static final double PULLBACK_MOM20_MAX = 0.25;

	/**
	 * 模板B 强势回踩低吸：EMA20 乖离下限（未破位）
	 */
	public static final double PULLBACK_EMA_SPREAD_MIN = -0.02;

	/**
	 * 模板B 强势回踩低吸：EMA20 乖离上限（回踩到位）
	 */
	public static final double PULLBACK_EMA_SPREAD_MAX = 0.05;

	/**
	 * 模板B 强势回踩低吸：量比上限（缩量回踩）
	 */
	public static final double PULLBACK_VOL_RATIO_MAX = 0.9;

	/**
	 * 模板B 强势回踩低吸：5日动量下限（回调不是崩塌）
	 */
	public static final double PULLBACK_MOM5_MIN = -0.05;

	/**
	 * 模板C 趋势加速：MACD 金叉位置约束（|dif|/close 上限，0轴附近）
	 */
	public static final double TREND_DIF_NORM_MAX = 0.02;

	/**
	 * 模板C 趋势加速：RSI6 下限
	 */
	public static final double TREND_RSI_MIN = 40;

	/**
	 * 模板C 趋势加速：RSI6 上限
	 */
	public static final double TREND_RSI_MAX = 65;

	/**
	 * 模板D 超跌反转：RSI6 超卖阈值
	 */
	public static final double OVERSOLD_RSI = 25;

	/**
	 * 模板D 超跌反转：5日动量深跌阈值
	 */
	public static final double OVERSOLD_MOM5 = -0.12;

	/**
	 * 模板D 超跌反转：反转需当日放量确认的当日量比下限（当日量/前5日均量）
	 */
	public static final double OVERSOLD_VOL_RATIO_MIN = 1.2;

	// ==================== 质量加分 ====================

	/**
	 * 板块共振：行业板块当日涨幅 ≥ 该值 强共振加分
	 */
	public static final double SECTOR_RET_STRONG = 0.02;

	/**
	 * 板块共振：行业板块当日涨幅 ≥ 该值 中等共振加分
	 */
	public static final double SECTOR_RET_MID = 0.01;

	/**
	 * 板块共振：行业板块当日跌幅 ≥ 该值 反共振减分
	 */
	public static final double SECTOR_RET_WEAK = -0.01;

	public static final double SECTOR_BONUS_STRONG = 12;

	public static final double SECTOR_BONUS_MID = 6;

	public static final double SECTOR_PENALTY_WEAK = -8;

	/**
	 * 主力资金：3日净流入为正且连续流入 ≥2 天 加分
	 */
	public static final double FLOW_BONUS = 10;

	/**
	 * 主力资金：3日净流出 减分
	 */
	public static final double FLOW_PENALTY = -10;

	/**
	 * 龙虎榜：近5个交易日（按交易日口径）净买 加分
	 */
	public static final double DRAGON_BONUS = 8;

	/**
	 * 龙虎榜：近5个交易日净卖 减分
	 */
	public static final double DRAGON_PENALTY = -8;

	/**
	 * 反追高：20日动量超过该值 重罚
	 */
	public static final double ANTI_CHASE_MOM20 = 0.25;

	public static final double ANTI_CHASE_MOM20_PENALTY = -15;

	/**
	 * 反追高：MA5/MA20 乖离超过该值 减分
	 */
	public static final double ANTI_CHASE_MA_RATIO = 0.10;

	public static final double ANTI_CHASE_MA_RATIO_PENALTY = -10;

	/**
	 * 连板惩罚：≥2连板且仍封板（买不进+隔日风险）
	 */
	public static final int CONSEC_LIMIT_UP = 2;

	public static final double CONSEC_LIMIT_UP_DISTANCE = 0.005;

	public static final double CONSEC_LIMIT_PENALTY = -15;

	// ==================== 入池线与规模控制 ====================

	/**
	 * 入池线：综合打分下限（精度优先，宁缺毋滥）
	 */
	public static final double MIN_SCORE = 65;

	/**
	 * 每日入候选池最大数量
	 */
	public static final int TOP_N = 3;

	/**
	 * 特征计算最少K线数（过滤次新+保证指标有效）
	 */
	public static final int MIN_BARS = 30;

	/**
	 * 特征计算回看K线数
	 */
	public static final int LOOKBACK_BARS = 70;

}
