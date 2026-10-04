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
 * 限售解禁（gate 解禁否决输入；tushare share_float 优先，float_ratio 百分点→小数）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_restricted_release")
@Schema(description = "限售解禁")
@EqualsAndHashCode(callSuper = true)
public class RestrictedReleaseEntity extends BaseEntity {

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
	 * 解禁时间
	 */
	@Schema(description = "解禁时间")
	private LocalDate planDate;

	/**
	 * 解禁数量（股）
	 */
	@Schema(description = "解禁数量（股）")
	private BigDecimal shares;

	/**
	 * 实际解禁市值（元，em 口径）
	 */
	@Schema(description = "实际解禁市值（元，em 口径）")
	private BigDecimal marketValue;

	/**
	 * 占总股本比例（小数，tushare 口径）
	 */
	@Schema(description = "占总股本比例（小数，tushare 口径）")
	private BigDecimal floatRatio;

	/**
	 * 占解禁前流通市值比例（小数；em 真口径，tushare 模式下为占总股本近似，gate 消费）
	 */
	@Schema(description = "占解禁前流通市值比例（小数；em 真口径，tushare 模式下为占总股本近似，gate 消费）")
	private BigDecimal floatMvRatio;

	/**
	 * 来源 tushare/em
	 */
	@Schema(description = "来源 tushare/em")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
