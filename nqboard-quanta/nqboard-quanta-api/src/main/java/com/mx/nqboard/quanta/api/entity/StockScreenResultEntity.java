package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 每日筛选打分结果（Stage 0 粗滤 + Stage 0.5 硬门 + Stage 1 模板打分）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_screen_result")
@Schema(description = "每日筛选打分结果")
@EqualsAndHashCode(callSuper = true)
public class StockScreenResultEntity extends Model<StockScreenResultEntity> {

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
	 * 信号日（打分依据日）YYYYMMDD
	 */
	@Schema(description = "信号日（打分依据日）YYYYMMDD")
	private String tradeDate;

	/**
	 * TS股票代码
	 */
	@Schema(description = "TS股票代码")
	private String tsCode;

	/**
	 * 股票名称
	 */
	@Schema(description = "股票名称")
	private String name;

	/**
	 * 综合打分（模板分+上下文加分，满分100）
	 */
	@Schema(description = "综合打分（模板分+上下文加分，满分100）")
	private java.math.BigDecimal screenScore;

	/**
	 * 命中模板 breakout/pullback/trend_accel/oversold/none
	 */
	@Schema(description = "命中模板 breakout/pullback/trend_accel/oversold/none")
	private String pattern;

	/**
	 * 模板基础分
	 */
	@Schema(description = "模板基础分")
	private java.math.BigDecimal patternScore;

	/**
	 * 是否通过打分入池线 1:通过 0:未通过
	 */
	@Schema(description = "是否通过打分入池线 1:通过 0:未通过")
	private String passed;

	/**
	 * 硬门否决原因（多个分号分隔）
	 */
	@Schema(description = "硬门否决原因（多个分号分隔）")
	private String rejectReason;

	/**
	 * 特征向量（动量/量比/RSI/trend_strength等）JSON
	 */
	@Schema(description = "特征向量 JSON")
	private String metrics;

}
