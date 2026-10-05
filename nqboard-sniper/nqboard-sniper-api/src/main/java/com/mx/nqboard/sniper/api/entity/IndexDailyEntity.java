package com.mx.nqboard.sniper.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 指数日行情（市场门 5 日涨幅、台账 000300/000905/399006 基准消费；独立于 sniper_daily_price——
 * 000905.SH 指数与 000905.SZ 个股 6 位代码冲突，同表 UK(code,trade_date,adjust) 会互相覆盖，
 * 2026-10-04 勘误，附录 A #20）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/05
 */
@Data
@TableName("sniper_index_daily")
@Schema(description = "指数日行情")
@EqualsAndHashCode(callSuper = true)
public class IndexDailyEntity extends BaseEntity {

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
	 * 指数代码前6位(000300/000905/399006)
	 */
	@Schema(description = "指数代码前6位(000300/000905/399006)")
	private String indexCode;

	/**
	 * 交易日
	 */
	@Schema(description = "交易日")
	private LocalDate tradeDate;

	/**
	 * 开盘价
	 */
	@Schema(description = "开盘价")
	private BigDecimal open;

	/**
	 * 最高价
	 */
	@Schema(description = "最高价")
	private BigDecimal high;

	/**
	 * 最低价
	 */
	@Schema(description = "最低价")
	private BigDecimal low;

	/**
	 * 收盘价
	 */
	@Schema(description = "收盘价")
	private BigDecimal close;

	/**
	 * 成交量(手,tushare index_daily原样)
	 */
	@Schema(description = "成交量(手,tushare index_daily原样)")
	private BigDecimal volume;

	/**
	 * 成交额(千元,tushare index_daily原样)
	 */
	@Schema(description = "成交额(千元,tushare index_daily原样)")
	private BigDecimal amount;

	/**
	 * 来源
	 */
	@Schema(description = "来源")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
