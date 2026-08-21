package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 盘后流水线执行日志
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("quant_pipeline_log")
@Schema(description = "盘后流水线执行日志")
@EqualsAndHashCode(callSuper = true)
public class QuantPipelineLogEntity extends Model<QuantPipelineLogEntity> {

    private static final long serialVersionUID = 1L;

	/**
	 * 业务id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "业务id")
	private Long id;

	/**
	 * 创建人
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 修改人
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 修改时间
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	/**
	 * 0-正常，1-删除
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

	/**
	 * 排序字段
	 */
	@Schema(description = "排序字段")
	private Integer orderNum;

	/**
	 * 本次流水线运行id
	 */
	@Schema(description = "本次流水线运行id")
	private String runId;

	/**
	 * 运行日期 YYYYMMDD
	 */
	@Schema(description = "运行日期 YYYYMMDD")
	private String runDate;

	/**
	 * 步骤编码 index_daily/stock_daily/...
	 */
	@Schema(description = "步骤编码")
	private String step;

	/**
	 * 步骤名称
	 */
	@Schema(description = "步骤名称")
	private String stepName;

	/**
	 * RUNNING/SUCCESS/FAILED/SKIPPED
	 */
	@Schema(description = "执行状态 RUNNING/SUCCESS/FAILED/SKIPPED")
	private String status;

	/**
	 * 影响行数（同步条数等）
	 */
	@Schema(description = "影响行数")
	private Integer affected;

	/**
	 * 执行说明
	 */
	@Schema(description = "执行说明")
	private String message;

	/**
	 * 异常信息（截断）
	 */
	@Schema(description = "异常信息（截断）")
	private String exception;

	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	private LocalDateTime beginTime;

	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	private LocalDateTime endTime;

	/**
	 * 耗时毫秒
	 */
	@Schema(description = "耗时毫秒")
	private Long elapsedMs;
}
