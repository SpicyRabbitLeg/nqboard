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
 * 复权因子（qfq 重算 + 除权检测；adj_factor 支持 trade_date 全市场一次拉）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_adj_factor")
@Schema(description = "复权因子")
@EqualsAndHashCode(callSuper = true)
public class AdjFactorEntity extends BaseEntity {

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
	 * 复权因子
	 */
	@Schema(description = "复权因子")
	private BigDecimal factor;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
