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
 * 龙虎榜（dragon_tiger 输入；tushare top_list 按日全市场 / em 区间兜底，金额单位元）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_dragon_tiger")
@Schema(description = "龙虎榜")
@EqualsAndHashCode(callSuper = true)
public class DragonTigerEntity extends BaseEntity {

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
	 * 上榜原因
	 */
	@Schema(description = "上榜原因")
	private String reason;

	/**
	 * 净买额（元）
	 */
	@Schema(description = "净买额（元）")
	private BigDecimal netBuy;

	/**
	 * 买入额（元）
	 */
	@Schema(description = "买入额（元）")
	private BigDecimal buyAmt;

	/**
	 * 卖出额（元）
	 */
	@Schema(description = "卖出额（元）")
	private BigDecimal sellAmt;

	/**
	 * 当日涨跌幅%
	 */
	@Schema(description = "当日涨跌幅%")
	private BigDecimal changePct;

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
