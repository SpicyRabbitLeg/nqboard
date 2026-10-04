package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 台账平仓原因（sniper_ledger_position.close_reason；止损全程优先于到期，非先到先得）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum ExitReasonEnum implements IEnum<String> {

	/**
	 * 止损（T+2 起 low ≤ 止损价，成交价=min(当日开盘,止损价)）
	 */
	STOP_LOSS("stop_loss"),

	/**
	 * 到期平仓（T+5 收盘，停牌顺延）
	 */
	HORIZON("horizon"),

	/**
	 * 低开放弃（T+1 低开 ≥2%，市场否定信号）
	 */
	GAP_ABORT("gap_abort"),

	/**
	 * 未成交（>10 个交易日无 bar，第 11 天关闭）
	 */
	NEVER_FILLED("never_filled");

	@JsonValue
	private final String value;

}
