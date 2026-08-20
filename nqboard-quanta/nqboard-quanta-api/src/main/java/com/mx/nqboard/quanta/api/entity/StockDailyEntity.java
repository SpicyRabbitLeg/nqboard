package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * Tushare日线行情
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_daily")
@Schema(description = "Tushare日线行情")
@EqualsAndHashCode(callSuper = true)
public class StockDailyEntity extends Model<StockDailyEntity> {

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
	 * 股票代码 002594.SZ
	 */
	@Schema(description = "股票代码")
	private String tsCode;

	/**
	 * 交易日期
	 */
	@Schema(description = "交易日期")
	private String tradeDate;

	/**
	 * 开盘价
	 */
	@Schema(description = "开盘价")
	private Float open;

	/**
	 * 最高价
	 */
	@Schema(description = "最高价")
	private Float high;

	/**
	 * 最低价
	 */
	@Schema(description = "最低价")
	private Float low;

	/**
	 * 收盘价
	 */
	@Schema(description = "收盘价")
	private Float close;

	/**
	 * 昨收价【除权价】
	 */
	@Schema(description = "昨收价【除权价】")
	private Float preClose;

	/**
	 * 涨跌额
	 * <p>change 为 MySQL 保留字，必须反引号包裹列名
	 */
	@TableField("`change`")
	@Schema(description = "涨跌额")
	private Float change;

	/**
	 * 涨跌幅（%） 【基于除权后的昨收计算的涨跌幅：（今收-除权昨收）/除权昨收 】
	 */
	@Schema(description = "涨跌幅（%）")
	private Float pctChg;

	/**
	 * 成交量 （手）
	 */
	@Schema(description = "成交量 （手）")
	private Float vol;

	/**
	 * 成交额 （千元）
	 */
	@Schema(description = "成交额 （千元）")
	private Float amount;

	/**
	 * 盘后成交量 （手）
	 */
	@Schema(description = "盘后成交量 （手）")
	private Float ahVol;

	/**
	 * 盘后成交额 （千元）
	 */
	@Schema(description = "盘后成交额 （千元）")
	private Float ahAmount;
}
