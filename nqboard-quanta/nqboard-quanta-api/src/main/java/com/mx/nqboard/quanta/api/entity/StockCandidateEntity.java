package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * <p>
 * 候选股票池
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_candidate")
@Schema(description = "候选股票池")
@EqualsAndHashCode(callSuper = true)
public class StockCandidateEntity extends Model<StockCandidateEntity> {

    private static final long serialVersionUID = 1L;

	/**
	 * 业务id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "业务id")
	private Long id;

	/**
	 * 创建人
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 修改人
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 修改时间
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	/**
	 * 0-正常，1-删除
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

	/**
	 * 排序字段
	 */
	@Schema(description = "排序字段")
	private Integer orderNum;

	/**
	 * 入选日（信号日）YYYYMMDD
	 */
	@Schema(description = "入选日（信号日）YYYYMMDD")
	private String tradeDate;

	/**
	 * TS股票代码
	 */
	@Schema(description = "TS股票代码")
	private String tsCode;

	/**
	 * 股票名称
	 */
	@Schema(description = "股票名称")
	private String name;

	/**
	 * 规则打分
	 */
	@Schema(description = "规则打分")
	private java.math.BigDecimal screenScore;

	/**
	 * LLM加权分（P5接入）
	 */
	@Schema(description = "LLM加权分")
	private java.math.BigDecimal llmScore;

	/**
	 * 综合置信度 0-100
	 */
	@Schema(description = "综合置信度 0-100")
	private Integer confidence;

	/**
	 * entry_ok/watch/avoid
	 */
	@Schema(description = "action entry_ok/watch/avoid")
	private String action;

	/**
	 * 命中模板
	 */
	@Schema(description = "命中模板")
	private String pattern;

	/**
	 * 看多理由标签列表 JSON
	 */
	@Schema(description = "看多理由标签列表 JSON")
	private String reasons;

	/**
	 * 各Agent结论摘要 JSON（P5）
	 */
	@Schema(description = "各Agent结论摘要 JSON")
	private String agentSummary;

	/**
	 * 离场计划 JSON（止损/止盈/最大持有天数）
	 */
	@Schema(description = "离场计划 JSON")
	private String exitPlan;

	/**
	 * 沪深300 5日收益
	 */
	@TableField("market_ret_5d")
	@Schema(description = "沪深300 5日收益")
	private java.math.BigDecimal marketRet5d;

	/**
	 * agent/rules_fallback
	 */
	@Schema(description = "决策模式 agent/rules_fallback")
	private String decisionMode;

	/**
	 * ACTIVE/EXPIRED/REMOVED
	 */
	@Schema(description = "状态 ACTIVE/EXPIRED/REMOVED")
	private String status;

	/**
	 * 过期日（入选日+5交易日）
	 */
	@Schema(description = "过期日 YYYYMMDD")
	private String expireDate;

}
