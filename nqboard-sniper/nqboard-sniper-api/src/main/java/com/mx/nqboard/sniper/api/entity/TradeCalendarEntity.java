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
 * 交易日历（磁盘缓存等价物；预灌 1990~至今+400天）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_trade_calendar")
@Schema(description = "交易日历")
@EqualsAndHashCode(callSuper = true)
public class TradeCalendarEntity extends BaseEntity {

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
	 * 日历日期
	 */
	@Schema(description = "日历日期")
	private LocalDate calDate;

	/**
	 * 1=交易日
	 */
	@Schema(description = "1=交易日")
	private Integer isOpen;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}
