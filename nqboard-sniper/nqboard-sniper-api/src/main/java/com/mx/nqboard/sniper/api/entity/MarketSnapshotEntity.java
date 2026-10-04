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
 * 全市场快照（Stage0 输入；每日收盘后一行/票）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_market_snapshot")
@Schema(description = "全市场快照")
@EqualsAndHashCode(callSuper = true)
public class MarketSnapshotEntity extends BaseEntity {

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
	 * 交易日
	 */
	@Schema(description = "交易日")
	private LocalDate tradeDate;

	/**
	 * 6位代码
	 */
	@Schema(description = "6位代码")
	private String code;

	/**
	 * 证券简称
	 */
	@Schema(description = "证券简称")
	private String name;

	/**
	 * 最新价
	 */
	@Schema(description = "最新价")
	private BigDecimal price;

	/**
	 * 涨跌幅%
	 */
	@Schema(description = "涨跌幅%")
	private BigDecimal changePct;

	/**
	 * 今开
	 */
	@Schema(description = "今开")
	private BigDecimal open;

	/**
	 * 最高
	 */
	@Schema(description = "最高")
	private BigDecimal high;

	/**
	 * 最低
	 */
	@Schema(description = "最低")
	private BigDecimal low;

	/**
	 * 昨收
	 */
	@Schema(description = "昨收")
	private BigDecimal prevClose;

	/**
	 * 成交量（手）
	 */
	@Schema(description = "成交量（手）")
	private BigDecimal volume;

	/**
	 * 成交额（元）
	 */
	@Schema(description = "成交额（元）")
	private BigDecimal amount;

	/**
	 * 换手率%
	 */
	@Schema(description = "换手率%")
	private BigDecimal turnoverRate;

	/**
	 * 来源 em/sina/tencent
	 */
	@Schema(description = "来源 em/sina/tencent")
	private String source;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
