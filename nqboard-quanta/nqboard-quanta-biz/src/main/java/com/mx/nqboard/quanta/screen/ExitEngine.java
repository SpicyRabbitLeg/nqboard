package com.mx.nqboard.quanta.screen;

/**
 * <p>
 * T+1 离场引擎（短线 3-5 日持有纪律，回测与模拟盘共用同一实现）
 * </p>
 * <p>
 * 六条规则（收盘触发、次日开盘成交的 T+1 真实化口径）：
 * <pre>
 * ① 止损：收盘 ≤ 买入价×0.95                 -> 次日开盘卖（stop_loss）
 * ② 保本止损：第3日起且期间浮盈曾≥+5%，
 *    止损线上移至买入价                       -> 收盘≤买入价次日开盘卖（breakeven_stop）
 * ③ 止盈：收盘 ≥ 买入价×1.15                 -> 次日开盘卖（take_profit）
 * ④ 弱势提前离场：第4日收盘仍 < 买入价        -> 次日开盘卖（weak_exit）
 * ⑤ 时间止损：持有 ≥5 个交易日               -> 次日开盘卖（time_exit）
 * ⑥ 开盘急杀：开盘 ≤ 买入价×0.95             -> 当日开盘直接卖，不等收盘（gap_stop）
 * </pre>
 * H7 入场检查（T+1 专属）：次日开盘跳空 >+5% 追高放弃；跳空 <-3% 隔夜利空放弃。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public final class ExitEngine {

	private ExitEngine() {
	}

	/**
	 * 最大持有交易日数（买入日=第0日）
	 */
	public static final int MAX_HOLD_DAYS = 5;

	/**
	 * 止损比例
	 */
	public static final double STOP_LOSS_PCT = 0.05;

	/**
	 * 止盈比例
	 */
	public static final double TAKE_PROFIT_PCT = 0.15;

	/**
	 * 保本止损触发浮盈（期间最高收盘相对买入价）
	 */
	public static final double BREAKEVEN_TRIGGER = 0.05;

	/**
	 * 保本止损生效起始日（第几日起）
	 */
	public static final int BREAKEVEN_FROM_DAY = 3;

	/**
	 * 弱势提前离场日（第几日收盘仍低于买入价）
	 */
	public static final int WEAK_EXIT_DAY = 4;

	/**
	 * 收盘离场评估（T+1：触发后由调用方在次日开盘成交）
	 * @param entryPrice 买入价
	 * @param heldDays 已持有交易日数（买入日=0）
	 * @param peakClose 持有期间最高收盘价
	 * @param close 当日收盘价
	 * @return 离场原因（stop_loss/breakeven_stop/take_profit/weak_exit/time_exit），null=继续持有
	 */
	public static String evaluateClose(double entryPrice, int heldDays, double peakClose, double close) {
		if (close <= entryPrice * (1 - STOP_LOSS_PCT)) {
			return "stop_loss";
		}
		boolean breakevenActive = heldDays >= BREAKEVEN_FROM_DAY
				&& peakClose >= entryPrice * (1 + BREAKEVEN_TRIGGER);
		if (breakevenActive && close <= entryPrice) {
			return "breakeven_stop";
		}
		if (close >= entryPrice * (1 + TAKE_PROFIT_PCT)) {
			return "take_profit";
		}
		if (heldDays >= WEAK_EXIT_DAY && close < entryPrice) {
			return "weak_exit";
		}
		if (heldDays >= MAX_HOLD_DAYS) {
			return "time_exit";
		}
		return null;
	}

	/**
	 * 开盘急杀判定（当日开盘直接卖出，不等收盘）
	 */
	public static boolean isGapStop(double entryPrice, double open) {
		return open <= entryPrice * (1 - STOP_LOSS_PCT);
	}

	/**
	 * H7 入场跳空检查：跳空 >+5%（追高）或 <-3%（隔夜利空）放弃买入
	 * @param prevClose 信号日收盘价
	 * @param open 次日开盘价
	 */
	public static boolean entryGapBlocked(double prevClose, double open) {
		return open > prevClose * (1 + ScreenConstants.H7_GAP_UP_LIMIT)
				|| open < prevClose * (1 + ScreenConstants.H7_GAP_DOWN_LIMIT);
	}

}
