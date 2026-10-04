package com.mx.nqboard.sniper.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * 台账每日 mark（update_marks 每日一行；全量重建幂等）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_ledger_mark")
@Schema(description = "台账每日mark")
@EqualsAndHashCode(callSuper = true)
public class LedgerMarkEntity extends BaseEntity {

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
	 * 仓位id
	 */
	@Schema(description = "仓位id")
	private Long positionId;

	/**
	 * 交易日
	 */
	@Schema(description = "交易日")
	private LocalDate tradeDate;

	/**
	 * 收盘价
	 */
	@Schema(description = "收盘价")
	private BigDecimal close;

	/**
	 * 自入场收益
	 */
	@Schema(description = "自入场收益")
	private BigDecimal retFromEntry;

	/**
	 * 相对信号日收盘收益
	 */
	@Schema(description = "相对信号日收盘收益")
	private BigDecimal retFromT0;

	/**
	 * 沪深300收盘
	 */
	@Schema(description = "沪深300收盘")
	private BigDecimal benchClose;

	/**
	 * 沪深300区间收益
	 */
	@Schema(description = "沪深300区间收益")
	private BigDecimal benchRet;

	/**
	 * 超额收益
	 */
	@Schema(description = "超额收益")
	private BigDecimal excessRet;

}
