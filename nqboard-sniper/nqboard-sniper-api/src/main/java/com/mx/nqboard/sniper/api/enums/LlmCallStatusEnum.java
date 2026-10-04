package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * LLM 调用状态（sniper_llm_call_log.status；失败也必须落行，是 fail-closed 可审计的前提）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum LlmCallStatusEnum implements IEnum<String> {

	/**
	 * 成功
	 */
	OK("ok"),

	/**
	 * 输出非法（HTTP 200 但 outputs 缺字段/非法 JSON）
	 */
	PARSE_ERROR("parse_error"),

	/**
	 * 超时
	 */
	TIMEOUT("timeout"),

	/**
	 * HTTP 错误（非 200/网络错误）
	 */
	HTTP_ERROR("http_error");

	@JsonValue
	private final String value;

}
