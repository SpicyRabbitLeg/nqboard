package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 每日任务状态（sniper_daily_run.status；Job 开始写 running，结束改 done/failed）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum RunStatusEnum implements IEnum<String> {

	/**
	 * 运行中
	 */
	RUNNING("running"),

	/**
	 * 成功
	 */
	DONE("done"),

	/**
	 * 失败
	 */
	FAILED("failed");

	@JsonValue
	private final String value;

}
