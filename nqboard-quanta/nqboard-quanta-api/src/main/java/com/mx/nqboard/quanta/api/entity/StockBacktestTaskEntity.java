package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 回测任务
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_backtest_task")
@Schema(description = "回测任务")
@EqualsAndHashCode(callSuper = true)
public class StockBacktestTaskEntity extends Model<StockBacktestTaskEntity> {

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
	 * 回测参数 JSON（universe/days/minScore/capital等）
	 */
	@Schema(description = "回测参数 JSON")
	private String params;

	/**
	 * PENDING/RUNNING/DONE/FAILED
	 */
	@Schema(description = "状态 PENDING/RUNNING/DONE/FAILED")
	private String status;

	/**
	 * 进度百分比 0-100
	 */
	@Schema(description = "进度百分比 0-100")
	private Integer progress;

	/**
	 * 统计结果 JSON（胜率/盈亏比/回撤/离场分布/分桶校准）
	 */
	@Schema(description = "统计结果 JSON")
	private String stats;

	/**
	 * 权益曲线 JSON [{date,equity}]
	 */
	@Schema(description = "权益曲线 JSON")
	private String equityCurve;

	/**
	 * 失败原因
	 */
	@Schema(description = "失败原因")
	private String errorMsg;

}
