package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 新闻情绪预打标（sniper_company_news.sentiment，关键词规则入库时确定）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum SentimentEnum implements IEnum<String> {

	/**
	 * 正面
	 */
	POSITIVE("positive"),

	/**
	 * 负面
	 */
	NEGATIVE("negative"),

	/**
	 * 中性
	 */
	NEUTRAL("neutral");

	@JsonValue
	private final String value;

}
