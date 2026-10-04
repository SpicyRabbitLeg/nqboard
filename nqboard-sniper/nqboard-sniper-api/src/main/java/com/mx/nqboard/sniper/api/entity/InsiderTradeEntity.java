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
 * 高管/股东增减持（sentiment insider 腿；tushare stk_holdertrade 优先，DE 减持为负）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_insider_trade")
@Schema(description = "股东增减持")
@EqualsAndHashCode(callSuper = true)
public class InsiderTradeEntity extends BaseEntity {

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
	 * 公告日期（demat_date 兜底）
	 */
	@Schema(description = "公告日期（demat_date 兜底）")
	private LocalDate annDate;

	/**
	 * 变动股东
	 */
	@Schema(description = "变动股东")
	private String holderName;

	/**
	 * 股东类型
	 */
	@Schema(description = "股东类型")
	private String holderType;

	/**
	 * 变动数量（股；DE 减持为负，与 Python 口径一致）
	 */
	@Schema(description = "变动数量（股；DE 减持为负，与 Python 口径一致）")
	private BigDecimal changeVol;

	/**
	 * 成交均价（元/股）
	 */
	@Schema(description = "成交均价（元/股）")
	private BigDecimal avgPrice;

	/**
	 * 变动后持股（股）
	 */
	@Schema(description = "变动后持股（股）")
	private BigDecimal afterShares;

	/**
	 * 来源 tushare/ths
	 */
	@Schema(description = "来源 tushare/ths")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
