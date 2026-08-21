package com.mx.nqboard.quanta.screen;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 入场模板枚举（四选一，模板分即入场资格）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Getter
@AllArgsConstructor
public enum ScreenPatternEnum {

	/**
	 * 突破启动（精度最高，主力模板）
	 */
	BREAKOUT("breakout", "突破启动", 45),

	/**
	 * 强势回踩低吸（胜率模板）
	 */
	PULLBACK("pullback", "强势回踩低吸", 40),

	/**
	 * 趋势加速（顺势持有型）
	 */
	TREND_ACCEL("trend_accel", "趋势加速", 35),

	/**
	 * 超跌反转（降级模板，分数低只能靠加分挤进TopN）
	 */
	OVERSOLD("oversold", "超跌反转", 25);

	/**
	 * 模板编码（落库值）
	 */
	private final String code;

	/**
	 * 模板名称
	 */
	private final String label;

	/**
	 * 模板基础分
	 */
	private final double baseScore;

}
