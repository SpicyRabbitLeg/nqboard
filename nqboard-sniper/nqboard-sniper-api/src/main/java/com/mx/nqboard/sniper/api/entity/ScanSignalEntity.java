package com.mx.nqboard.sniper.api.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import com.mx.nqboard.sniper.api.enums.ScanActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 扫描信号明细（列对齐 BuySignal，src/scan/models.py；报告重跑按 report_id 先删后插）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_scan_signal")
@Schema(description = "扫描信号明细")
@EqualsAndHashCode(callSuper = true)
public class ScanSignalEntity extends BaseEntity {

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
	 * 报告id
	 */
	@Schema(description = "报告id")
	private Long reportId;

	/**
	 * 股票代码，如 600519.SH
	 */
	@Schema(description = "股票代码，如 600519.SH")
	private String ticker;

	/**
	 * 信号动作（永不输出 sell）
	 */
	@Schema(description = "信号动作")
	private ScanActionEnum action;

	/**
	 * 置信度 0-100
	 */
	@Schema(description = "置信度 0-100")
	private BigDecimal confidence;

	/**
	 * 加权得分
	 */
	@Schema(description = "加权得分")
	private BigDecimal weightedScore;

	/**
	 * 建议股数
	 */
	@Schema(description = "建议股数")
	private Integer quantity;

	/**
	 * 占用资金（元）
	 */
	@Schema(description = "占用资金（元）")
	private BigDecimal trialCashUsed;

	/**
	 * 决策理由（截断200字，与 BuySignal 一致）
	 */
	@Schema(description = "决策理由（截断200字，与 BuySignal 一致）")
	private String reasoning;

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
	 * 数据预检警告
	 */
	@Schema(description = "数据预检警告")
	private String prefetchWarnings;

}
