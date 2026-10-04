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
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 日行情（核心表，双复权口径；UK(code,trade_date,adjust) 即防重）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_daily_price")
@Schema(description = "日行情")
@EqualsAndHashCode(callSuper = true)
public class DailyPriceEntity extends BaseEntity {

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
	 * 6位代码
	 */
	@Schema(description = "6位代码")
	private String code;

	/**
	 * 交易日
	 */
	@Schema(description = "交易日")
	private LocalDate tradeDate;

	/**
	 * 复权口径
	 */
	@Schema(description = "复权口径")
	private AdjustEnum adjust;

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
	 * 除权调整后昨收（仅 none 行，来自 tushare daily）
	 */
	@Schema(description = "除权调整后昨收（仅 none 行，来自 tushare daily）")
	private BigDecimal preClose;

	/**
	 * 成交量（统一为手；新浪股在入库前÷100，原样存）
	 */
	@Schema(description = "成交量（统一为手；新浪股在入库前÷100，原样存）")
	private BigDecimal volume;

	/**
	 * 成交额（tushare=千元，东财/腾讯=元，原样存）
	 */
	@Schema(description = "成交额（tushare=千元，东财/腾讯=元，原样存）")
	private BigDecimal amount;

	/**
	 * 来源 tushare/em/tencent/sina
	 */
	@Schema(description = "来源 tushare/em/tencent/sina")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
