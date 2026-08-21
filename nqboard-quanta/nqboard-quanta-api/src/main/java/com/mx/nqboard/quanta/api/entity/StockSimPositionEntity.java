package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * <p>
 * 模拟持仓
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_sim_position")
@Schema(description = "模拟持仓")
@EqualsAndHashCode(callSuper = true)
public class StockSimPositionEntity extends Model<StockSimPositionEntity> {

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
	 * 来源候选记录id
	 */
	@Schema(description = "来源候选记录id")
	private Long candidateId;

	/**
	 * TS股票代码
	 */
	@Schema(description = "TS股票代码")
	private String tsCode;

	/**
	 * 信号日（PENDING_BUY 期间为信号日，成交后更新为实际买入日）
	 */
	@Schema(description = "买入日 YYYYMMDD")
	private String buyDate;

	/**
	 * 买入价（次日开盘价）
	 */
	@Schema(description = "买入价")
	private java.math.BigDecimal buyPrice;

	/**
	 * 买入数量（100股整手）
	 */
	@Schema(description = "买入数量")
	private Integer qty;

	/**
	 * 买入成本（含佣金）
	 */
	@Schema(description = "买入成本")
	private java.math.BigDecimal cost;

	/**
	 * 止损价（买入价×0.95）
	 */
	@Schema(description = "止损价")
	private java.math.BigDecimal stopPrice;

	/**
	 * 止盈价（买入价×1.15）
	 */
	@Schema(description = "止盈价")
	private java.math.BigDecimal targetPrice;

	/**
	 * 最大持有交易日数
	 */
	@Schema(description = "最大持有交易日数")
	private Integer maxHoldDays;

	/**
	 * PENDING_BUY/HOLDING/PENDING_SELL/EXITED/CANCELLED
	 */
	@Schema(description = "状态 PENDING_BUY/HOLDING/PENDING_SELL/EXITED/CANCELLED")
	private String status;

	/**
	 * 卖出日
	 */
	@Schema(description = "卖出日 YYYYMMDD")
	private String exitDate;

	/**
	 * 卖出价
	 */
	@Schema(description = "卖出价")
	private java.math.BigDecimal exitPrice;

	/**
	 * 离场原因
	 */
	@Schema(description = "离场原因")
	private String exitReason;

	/**
	 * 卖出净得（扣佣金印花税）
	 */
	@Schema(description = "卖出净得")
	private java.math.BigDecimal proceeds;

	/**
	 * 盈亏额
	 */
	@Schema(description = "盈亏额")
	private java.math.BigDecimal pnl;

	/**
	 * 收益率
	 */
	@Schema(description = "收益率")
	private java.math.BigDecimal ret;

	/**
	 * 持有交易日数
	 */
	@Schema(description = "持有交易日数")
	private Integer heldDays;

}
