package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 台账仓位状态（sniper_ledger_position.status；gap_abort/never_filled 是 closed+close_reason，不占 status）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum PositionStatusEnum implements IEnum<String> {

	/**
	 * 待入场
	 */
	PENDING_ENTRY("pending_entry"),

	/**
	 * 持仓中
	 */
	OPEN("open"),

	/**
	 * 已平仓
	 */
	CLOSED("closed");

	@JsonValue
	private final String value;

}
