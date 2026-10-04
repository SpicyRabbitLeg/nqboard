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
import com.mx.nqboard.sniper.api.enums.ExitReasonEnum;
import com.mx.nqboard.sniper.api.enums.PositionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 模拟台账仓位（tracking.py TrackedPosition 等价物；UK(cohort_date,ticker) 幂等，
 * gap_abort/never_filled 是 closed+close_reason 不占 status）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_ledger_position")
@Schema(description = "模拟台账仓位")
@EqualsAndHashCode(callSuper = true)
public class LedgerPositionEntity extends BaseEntity {

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
	 * 信号日（cohort_id=scan_&lt;date&gt;）
	 */
	@Schema(description = "信号日（cohort_id=scan_<date>）")
	private LocalDate cohortDate;

	/**
	 * 来源报告标识（ingested_reports 幂等审计）
	 */
	@Schema(description = "来源报告标识（ingested_reports 幂等审计）")
	private String sourceReport;

	/**
	 * 股票代码，如 600519.SH
	 */
	@Schema(description = "股票代码，如 600519.SH")
	private String ticker;

	/**
	 * 仓位状态
	 */
	@Schema(description = "仓位状态")
	private PositionStatusEnum status;

	/**
	 * 入场日（T+1 首个交易日）
	 */
	@Schema(description = "入场日（T+1 首个交易日）")
	private LocalDate entryDate;

	/**
	 * 入场价（T+1 开盘价）
	 */
	@Schema(description = "入场价（T+1 开盘价）")
	private BigDecimal entryPrice;

	/**
	 * 入场成本（含费用，元）
	 */
	@Schema(description = "入场成本（含费用，元）")
	private BigDecimal entryCost;

	/**
	 * 信号日收盘（gap 判定基准）
	 */
	@Schema(description = "信号日收盘（gap 判定基准）")
	private BigDecimal signalDayClose;

	/**
	 * 股数
	 */
	@Schema(description = "股数")
	private Integer quantity;

	/**
	 * 占用资金（元）
	 */
	@Schema(description = "占用资金（元）")
	private BigDecimal trialCashUsed;

	/**
	 * 止损价（=entry_price×(1-stop_pct)）
	 */
	@Schema(description = "止损价（=entry_price×(1-stop_pct)）")
	private BigDecimal stopPrice;

	/**
	 * 止损启用日（防对已存活仓位回溯止损）
	 */
	@Schema(description = "止损启用日（防对已存活仓位回溯止损）")
	private LocalDate stopFromDate;

	/**
	 * 平仓日
	 */
	@Schema(description = "平仓日")
	private LocalDate closeDate;

	/**
	 * 平仓价
	 */
	@Schema(description = "平仓价")
	private BigDecimal closePrice;

	/**
	 * 平仓原因
	 */
	@Schema(description = "平仓原因")
	private ExitReasonEnum closeReason;

	/**
	 * 卖出净额（扣费，元）
	 */
	@Schema(description = "卖出净额（扣费，元）")
	private BigDecimal exitProceeds;

	/**
	 * 绝对盈亏（元）
	 */
	@Schema(description = "绝对盈亏（元）")
	private BigDecimal pnl;

	/**
	 * 盈亏比例
	 */
	@Schema(description = "盈亏比例")
	private BigDecimal pnlPct;

	/**
	 * 入场~平仓区间沪深300收益
	 */
	@Schema(description = "入场~平仓区间沪深300收益")
	private BigDecimal benchRetFull;

	/**
	 * 超额收益
	 */
	@Schema(description = "超额收益")
	private BigDecimal excessRet;

	/**
	 * 最大浮盈
	 */
	@Schema(description = "最大浮盈")
	private BigDecimal maxGain;

	/**
	 * 最大回撤
	 */
	@Schema(description = "最大回撤")
	private BigDecimal maxDrawdown;

	/**
	 * 信号置信度
	 */
	@Schema(description = "信号置信度")
	private BigDecimal confidence;

	/**
	 * 信号加权得分
	 */
	@Schema(description = "信号加权得分")
	private BigDecimal weightedScore;

	/**
	 * Stage1 快筛分
	 */
	@Schema(description = "Stage1 快筛分")
	private BigDecimal screenScore;

	/**
	 * 偏多因子标签数组
	 */
	@Schema(description = "偏多因子标签数组")
	private String reasons;

	/**
	 * 风险标记数组
	 */
	@Schema(description = "风险标记数组")
	private String riskFlags;

	/**
	 * 分析师信号汇总
	 */
	@Schema(description = "分析师信号汇总")
	private String analystSummary;

	/**
	 * 信号日市场环境原始值（market_up/down 由复盘按 >=0 派生）
	 */
	@Schema(description = "信号日市场环境原始值（market_up/down 由复盘按 >=0 派生）")
	private BigDecimal marketRet5d;

}
