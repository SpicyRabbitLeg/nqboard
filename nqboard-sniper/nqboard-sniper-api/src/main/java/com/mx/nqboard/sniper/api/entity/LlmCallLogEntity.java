package com.mx.nqboard.sniper.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import com.mx.nqboard.sniper.api.enums.LlmCallStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * LLM 调用审计（P-07 缺口：token/成本可离线评估；成功失败都落行，是 fail-closed 可审计的前提）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_llm_call_log")
@Schema(description = "LLM调用审计")
@EqualsAndHashCode(callSuper = true)
public class LlmCallLogEntity extends BaseEntity {

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
	 * 调用时间
	 */
	@Schema(description = "调用时间")
	private LocalDateTime calledAt;

	/**
	 * 工作流 news_sentiment/policy_sentiment/flat_decision
	 */
	@Schema(description = "工作流 news_sentiment/policy_sentiment/flat_decision")
	private String workflow;

	/**
	 * 涉及股票数组
	 */
	@Schema(description = "涉及股票数组")
	private String tickers;

	/**
	 * 输入摘要（sha256 hex 前 64 位）
	 */
	@Schema(description = "输入摘要")
	private String inputDigest;

	/**
	 * 工作流输出 JSON
	 */
	@Schema(description = "工作流输出 JSON")
	private String outputJson;

	/**
	 * 调用状态
	 */
	@Schema(description = "调用状态")
	private LlmCallStatusEnum status;

	/**
	 * 耗时（毫秒）
	 */
	@Schema(description = "耗时（毫秒）")
	private Integer latencyMs;

	/**
	 * 错误摘要（≤500 字）
	 */
	@Schema(description = "错误摘要（≤500 字）")
	private String error;

}
