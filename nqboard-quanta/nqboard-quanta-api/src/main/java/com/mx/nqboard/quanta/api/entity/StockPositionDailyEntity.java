package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * <p>
 * 模拟持仓逐日盯市
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_position_daily")
@Schema(description = "模拟持仓逐日盯市")
@EqualsAndHashCode(callSuper = true)
public class StockPositionDailyEntity extends Model<StockPositionDailyEntity> {

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
	 * 持仓id
	 */
	@Schema(description = "持仓id")
	private Long positionId;

	/**
	 * 交易日期 YYYYMMDD
	 */
	@Schema(description = "交易日期 YYYYMMDD")
	private String tradeDate;

	/**
	 * 当日收盘价
	 */
	@Schema(description = "当日收盘价")
	private java.math.BigDecimal close;

	/**
	 * 当日盈亏
	 */
	@Schema(description = "当日盈亏")
	private java.math.BigDecimal dayPnl;

	/**
	 * 累计盈亏
	 */
	@Schema(description = "累计盈亏")
	private java.math.BigDecimal cumPnl;

	/**
	 * 累计收益率
	 */
	@Schema(description = "累计收益率")
	private java.math.BigDecimal cumRet;

	/**
	 * HOLD/PENDING_SELL/SELL
	 */
	@Schema(description = "动作 HOLD/PENDING_SELL/SELL")
	private String action;

	/**
	 * 动作说明
	 */
	@Schema(description = "动作说明")
	private String actionReason;

}
