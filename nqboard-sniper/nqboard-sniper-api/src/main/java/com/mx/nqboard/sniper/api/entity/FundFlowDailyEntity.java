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
 * 主力资金流（main_force_flow 输入；tushare moneyflow 优先，万元→元后入库，main_net 必须为元）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_fund_flow_daily")
@Schema(description = "主力资金流")
@EqualsAndHashCode(callSuper = true)
public class FundFlowDailyEntity extends BaseEntity {

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
	 * 主力净流入（元）
	 */
	@Schema(description = "主力净流入（元）")
	private BigDecimal mainNet;

	/**
	 * 超大单净流入（元）
	 */
	@Schema(description = "超大单净流入（元）")
	private BigDecimal superNet;

	/**
	 * 大单净流入（元）
	 */
	@Schema(description = "大单净流入（元）")
	private BigDecimal largeNet;

	/**
	 * 中单净流入（元）
	 */
	@Schema(description = "中单净流入（元）")
	private BigDecimal mediumNet;

	/**
	 * 小单净流入（元）
	 */
	@Schema(description = "小单净流入（元）")
	private BigDecimal smallNet;

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
