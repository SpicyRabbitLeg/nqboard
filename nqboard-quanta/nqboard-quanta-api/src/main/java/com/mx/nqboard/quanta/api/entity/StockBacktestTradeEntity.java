package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 回测成交明细
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_backtest_trade")
@Schema(description = "回测成交明细")
@EqualsAndHashCode(callSuper = true)
public class StockBacktestTradeEntity extends Model<StockBacktestTradeEntity> {

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
	 * 回测任务id
	 */
	@Schema(description = "回测任务id")
	private Long taskId;

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
	 * 命中模板
	 */
	@Schema(description = "命中模板")
	private String pattern;

	/**
	 * 买入日
	 */
	@Schema(description = "买入日")
	private String entryDate;

	/**
	 * 买入价（开盘价）
	 */
	@Schema(description = "买入价")
	private BigDecimal entryPrice;

	/**
	 * 卖出日
	 */
	@Schema(description = "卖出日")
	private String exitDate;

	/**
	 * 卖出价（开盘价）
	 */
	@Schema(description = "卖出价")
	private BigDecimal exitPrice;

	/**
	 * 数量
	 */
	@Schema(description = "数量")
	private Integer qty;

	/**
	 * 离场原因 stop_loss/take_profit/time_exit/gap_stop/breakeven_stop/weak_exit/open_at_end
	 */
	@Schema(description = "离场原因")
	private String reason;

	/**
	 * 盈亏额
	 */
	@Schema(description = "盈亏额")
	private BigDecimal pnl;

	/**
	 * 收益率
	 */
	@Schema(description = "收益率")
	private BigDecimal ret;

	/**
	 * 持有交易日数
	 */
	@Schema(description = "持有交易日数")
	private Integer heldDays;

	/**
	 * 信号日打分
	 */
	@Schema(description = "信号日打分")
	private BigDecimal signalScore;

}
