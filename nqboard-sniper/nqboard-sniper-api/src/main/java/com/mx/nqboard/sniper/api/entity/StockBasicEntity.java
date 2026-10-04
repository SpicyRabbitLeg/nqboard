package com.mx.nqboard.sniper.api.entity;

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
 * 股票基础信息（Tushare stock_basic 批量 + 东财 f127 行业修正）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_stock_basic")
@Schema(description = "股票基础信息")
@EqualsAndHashCode(callSuper = true)
public class StockBasicEntity extends BaseEntity {

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
	 * 交易所 SH/SZ/BJ
	 */
	@Schema(description = "交易所 SH/SZ/BJ")
	private String exchange;

	/**
	 * Tushare 代码，如 600519.SH
	 */
	@Schema(description = "Tushare 代码，如 600519.SH")
	private String tsCode;

	/**
	 * 证券简称（ST 过滤依据）
	 */
	@Schema(description = "证券简称")
	private String name;

	/**
	 * 上市日期（次新过滤）
	 */
	@Schema(description = "上市日期（次新过滤）")
	private LocalDate listDate;

	/**
	 * 行业（东财 f127 口径优先，与板块名对齐）
	 */
	@Schema(description = "行业（东财 f127 口径优先，与板块名对齐）")
	private String industry;

	/**
	 * 行业口径来源 em/tushare
	 */
	@Schema(description = "行业口径来源 em/tushare")
	private String industrySource;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
