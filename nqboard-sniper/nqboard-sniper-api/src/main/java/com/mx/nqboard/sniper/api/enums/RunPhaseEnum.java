package com.mx.nqboard.sniper.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 每日任务阶段（sniper_daily_run.phase；UK(run_date,phase) 即幂等键）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Getter
@AllArgsConstructor
public enum RunPhaseEnum implements IEnum<String> {

	/**
	 * 数据日更（15:05~15:15 五阶段）
	 */
	DATA_UPDATE("data_update"),

	/**
	 * 盘后扫描（15:35）
	 */
	SCAN("scan"),

	/**
	 * 台账跟踪（15:40）
	 */
	TRACK("track"),

	/**
	 * 周复盘（每周五 16:00）
	 */
	REVIEW("review");

	@JsonValue
	private final String value;

}
