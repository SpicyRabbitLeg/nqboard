package com.mx.nqboard.sniper.api.entity;

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
 * 扫描被拒明细（复盘对账核心；拒因 code() 字符串与 Python 版逐字节一致，报告重跑按 report_id 先删后插）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_scan_rejected")
@Schema(description = "扫描被拒明细")
@EqualsAndHashCode(callSuper = true)
public class ScanRejectedEntity extends BaseEntity {

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
	 * 拒因 code() 字符串数组
	 */
	@Schema(description = "拒因 code() 字符串数组")
	private String reasons;

	/**
	 * PM 决策对象
	 */
	@Schema(description = "PM 决策对象")
	private String decision;

	/**
	 * 预检警告数组
	 */
	@Schema(description = "预检警告数组")
	private String warnings;

}
