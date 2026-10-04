package com.mx.nqboard.sniper.api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;
import com.mx.nqboard.sniper.api.enums.RunStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 每日运行记账（Quartz Job 幂等键 UK(run_date,phase)；预算记账与缺数标记）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_daily_run")
@Schema(description = "每日运行记账")
@EqualsAndHashCode(callSuper = true)
public class DailyRunEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 雪花id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "雪花id")
	private Long id;

	/**
	 * 删除状态（0未删除、1删除）
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除状态（0未删除、1删除）")
	private String delFlag;

	/**
	 * 运行日期
	 */
	@Schema(description = "运行日期")
	private LocalDate runDate;

	/**
	 * 任务阶段
	 */
	@Schema(description = "任务阶段")
	private RunPhaseEnum phase;

	/**
	 * 运行状态
	 */
	@Schema(description = "运行状态")
	private RunStatusEnum status;

	/**
	 * 东财主源日预算消耗
	 */
	@Schema(description = "东财主源日预算消耗")
	private Integer budgetUsed;

	/**
	 * 缺数标记/统计
	 */
	@Schema(description = "缺数标记/统计")
	private String detail;

	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	private LocalDateTime startedAt;

	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	private LocalDateTime finishedAt;

}
