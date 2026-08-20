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
 * Tushare top_list 龙虎榜每日明细
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_top_list")
@Schema(description = "Tushare top_list 龙虎榜每日明细")
@EqualsAndHashCode(callSuper = true)
public class StockTopListEntity extends Model<StockTopListEntity> {

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
	 * 交易日期YYYYMMDD
	 */
	@Schema(description = "交易日期YYYYMMDD")
	private String tradeDate;

	/**
	 * TS代码
	 */
	@Schema(description = "TS代码")
	private String tsCode;

	/**
	 * 股票名称
	 */
	@Schema(description = "股票名称")
	private String name;

	/**
	 * 收盘价
	 */
	@Schema(description = "收盘价")
	private BigDecimal close;

	/**
	 * 涨跌幅%
	 */
	@Schema(description = "涨跌幅%")
	private BigDecimal pctChange;

	/**
	 * 换手率%
	 */
	@Schema(description = "换手率%")
	private BigDecimal turnoverRate;

	/**
	 * 总成交额(元)
	 */
	@Schema(description = "总成交额(元)")
	private BigDecimal amount;

	/**
	 * 龙虎榜卖出额
	 */
	@Schema(description = "龙虎榜卖出额")
	private BigDecimal lSell;

	/**
	 * 龙虎榜买入额
	 */
	@Schema(description = "龙虎榜买入额")
	private BigDecimal lBuy;

	/**
	 * 龙虎榜成交额
	 */
	@Schema(description = "龙虎榜成交额")
	private BigDecimal lAmount;

	/**
	 * 龙虎榜净买入额
	 */
	@Schema(description = "龙虎榜净买入额")
	private BigDecimal netAmount;

	/**
	 * 龙虎榜净买额占比
	 */
	@Schema(description = "龙虎榜净买额占比")
	private BigDecimal netRate;

	/**
	 * 龙虎榜成交额占比
	 */
	@Schema(description = "龙虎榜成交额占比")
	private BigDecimal amountRate;

	/**
	 * 当日流通市值
	 */
	@Schema(description = "当日流通市值")
	private BigDecimal floatValues;

	/**
	 * 上榜理由
	 */
	@Schema(description = "上榜理由")
	private String reason;
}
