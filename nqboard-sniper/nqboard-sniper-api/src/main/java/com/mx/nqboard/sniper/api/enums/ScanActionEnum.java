package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 扫描信号动作（sniper_scan_signal.action，PM 空仓合成输出，永不输出 sell）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum ScanActionEnum implements IEnum<String> {

	/**
	 * 可试仓
	 */
	ENTRY_OK("entry_ok"),

	/**
	 * 观望
	 */
	WATCH("watch"),

	/**
	 * 回避
	 */
	AVOID("avoid");

	@JsonValue
	private final String value;

}
