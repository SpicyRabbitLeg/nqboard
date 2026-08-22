package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 数据同步执行日志（Quartz 定时任务逐任务追溯）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("quant_sync_log")
@Schema(description = "数据同步执行日志")
@EqualsAndHashCode(callSuper = true)
public class QuantSyncLogEntity extends Model<QuantSyncLogEntity> {

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
	 * 任务编码 stock_basic/stock_daily/index_daily/...
	 */
	@Schema(description = "任务编码")
	private String syncType;

	/**
	 * 任务名称（股票日线同步等）
	 */
	@Schema(description = "任务名称")
	private String syncName;

	/**
	 * 运行日期 YYYYMMDD
	 */
	@Schema(description = "运行日期 YYYYMMDD")
	private String runDate;

	/**
	 * 处理总数（如待同步股票数/交易日数）
	 */
	@Schema(description = "处理总数")
	private Integer totalCount;

	/**
	 * 成功条数（落库影响行数）
	 */
	@Schema(description = "成功条数")
	private Integer successCount;

	/**
	 * 失败条数
	 */
	@Schema(description = "失败条数")
	private Integer failCount;

	/**
	 * 同步区间说明（如 20260815~20260822）
	 */
	@Schema(description = "同步区间说明")
	private String syncRange;

	/**
	 * RUNNING/SUCCESS/FAILED
	 */
	@Schema(description = "执行状态 RUNNING/SUCCESS/FAILED")
	private String status;

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
