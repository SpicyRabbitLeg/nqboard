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
 * 财务指标（同花顺 indicator + tushare daily_basic 估值合并，两腿各存一行）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_financial_indicator")
@Schema(description = "财务指标（双腿）")
@EqualsAndHashCode(callSuper = true)
public class FinancialIndicatorEntity extends BaseEntity {

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
	 * 报告期（腿1）或估值交易日（腿2）
	 */
	@Schema(description = "报告期（腿1）或估值交易日（腿2）")
	private LocalDate reportPeriod;

	/**
	 * 销售毛利率（小数）
	 */
	@Schema(description = "销售毛利率（小数）")
	private BigDecimal grossMargin;

	/**
	 * 销售净利率（小数）
	 */
	@Schema(description = "销售净利率（小数）")
	private BigDecimal netMargin;

	/**
	 * 净资产收益率（小数）
	 */
	@Schema(description = "净资产收益率（小数）")
	private BigDecimal roe;

	/**
	 * 营收增长率（小数）
	 */
	@Schema(description = "营收增长率（小数）")
	private BigDecimal revenueGrowth;

	/**
	 * 净利润增长率（小数）
	 */
	@Schema(description = "净利润增长率（小数）")
	private BigDecimal profitGrowth;

	/**
	 * 市盈率 TTM
	 */
	@Schema(description = "市盈率TTM")
	private BigDecimal peTtm;

	/**
	 * 市净率
	 */
	@Schema(description = "市净率")
	private BigDecimal pb;

	/**
	 * 市销率 TTM
	 */
	@Schema(description = "市销率TTM")
	private BigDecimal psTtm;

	/**
	 * 总市值（元，daily_basic total_mv 万元×1e4）
	 */
	@Schema(description = "总市值（元，daily_basic total_mv 万元×1e4）")
	private BigDecimal marketCap;

	/**
	 * 来源 ths/tushare
	 */
	@Schema(description = "来源 ths/tushare")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
