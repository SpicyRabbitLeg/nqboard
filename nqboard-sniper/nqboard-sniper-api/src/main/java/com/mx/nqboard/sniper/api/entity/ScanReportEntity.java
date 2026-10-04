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
 * 扫描报告头（每次运行一行；UK(as_of,universe) 保证幂等重跑覆盖）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_scan_report")
@Schema(description = "扫描报告头")
@EqualsAndHashCode(callSuper = true)
public class ScanReportEntity extends BaseEntity {

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
	 * 数据日期
	 */
	@Schema(description = "数据日期")
	private LocalDate asOf;

	/**
	 * 股票池 all/hs300/csi500/hs300_csi500
	 */
	@Schema(description = "股票池 all/hs300/csi500/hs300_csi500")
	private String universe;

	/**
	 * 决策模式
	 */
	@Schema(description = "决策模式")
	private String mode;

	/**
	 * Gate 版本
	 */
	@Schema(description = "Gate 版本")
	private String gateVersion;

	/**
	 * 沪深300五日涨幅
	 */
	@Schema(description = "沪深300五日涨幅")
	private BigDecimal marketRet5d;

	/**
	 * 市场门拦截（0否1是）
	 */
	@Schema(description = "市场门拦截（0否1是）")
	private String marketGateBlocked;

	/**
	 * 预检通过（0否1是）
	 */
	@Schema(description = "预检通过（0否1是）")
	private String prefetchOk;

	/**
	 * Stage0 候选数
	 */
	@Schema(description = "Stage0 候选数")
	private Integer stage0Count;

	/**
	 * Stage1 快筛通过数
	 */
	@Schema(description = "Stage1 快筛通过数")
	private Integer stage1Count;

	/**
	 * Stage2 深度分析数
	 */
	@Schema(description = "Stage2 深度分析数")
	private Integer stage2Count;

	/**
	 * 信号数
	 */
	@Schema(description = "信号数")
	private Integer signalCount;

	/**
	 * 预算/漏斗统计/prefetch_warnings
	 */
	@Schema(description = "预算/漏斗统计/prefetch_warnings")
	private String meta;

	/**
	 * 完整 to_dict() 快照（golden master 对拍用）
	 */
	@Schema(description = "完整 to_dict() 快照（golden master 对拍用）")
	private String reportJson;

	/**
	 * Markdown 渲染报告
	 */
	@Schema(description = "Markdown 渲染报告")
	private String reportMd;

}
