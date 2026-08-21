package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * LLM(Dify) 逐 Agent 分析结果
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_agent_analysis")
@Schema(description = "LLM逐Agent分析结果")
@EqualsAndHashCode(callSuper = true)
public class StockAgentAnalysisEntity extends Model<StockAgentAnalysisEntity> {

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
	 * 分析基准日 YYYYMMDD
	 */
	@Schema(description = "分析基准日 YYYYMMDD")
	private String tradeDate;

	/**
	 * TS股票代码
	 */
	@Schema(description = "TS股票代码")
	private String tsCode;

	/**
	 * 分析师标识 technical/sector/money_flow/dragon_tiger/news/policy
	 */
	@Schema(description = "分析师标识")
	private String agentKey;

	/**
	 * bullish/bearish/neutral/n/a
	 */
	@Schema(description = "信号 bullish/bearish/neutral/n/a")
	private String signal;

	/**
	 * 置信度 0-100
	 */
	@Schema(description = "置信度 0-100")
	private Integer confidence;

	/**
	 * 推理摘要
	 */
	@Schema(description = "推理摘要")
	private String reasoning;

	/**
	 * agent=LLM分析 rules_fallback=规则降级
	 */
	@Schema(description = "决策模式 agent/rules_fallback")
	private String decisionMode;

	/**
	 * 使用的模型
	 */
	@Schema(description = "使用的模型")
	private String modelName;

	/**
	 * 分析耗时毫秒
	 */
	@Schema(description = "分析耗时毫秒")
	private Integer latencyMs;

	/**
	 * 工具调用次数
	 */
	@Schema(description = "工具调用次数")
	private Integer toolCalls;

	/**
	 * Agent原始输出全文（调试用）
	 */
	@Schema(description = "Agent原始输出全文")
	private String rawOutput;

}
