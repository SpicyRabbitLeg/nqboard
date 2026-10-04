package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 复权口径（sniper_daily_price.adjust）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum AdjustEnum implements IEnum<String> {

	/**
	 * 不复权
	 */
	NONE("none"),

	/**
	 * 前复权
	 */
	QFQ("qfq");

	@JsonValue
	private final String value;

}
