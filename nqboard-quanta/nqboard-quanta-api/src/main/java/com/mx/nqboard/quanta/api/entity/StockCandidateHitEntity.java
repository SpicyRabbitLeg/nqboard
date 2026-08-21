package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 候选池信号命中率追踪
 * </p>
 * <p>
 * 口径：信号日 T 收盘出信号 -> T+1 开盘价入场（H7 跳空放弃记 entry_skipped）
 * -> fwd_Nd = T+N 收盘 / 入场价 - 1（毛收益，不含成本，用于信号质量排序）。
 * best_ret 为持有期内（T+1..T+5）最高收盘收益。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_candidate_hit")
@Schema(description = "候选池信号命中率追踪")
@EqualsAndHashCode(callSuper = true)
public class StockCandidateHitEntity extends Model<StockCandidateHitEntity> {

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
	 * 信号日 YYYYMMDD
	 */
	@Schema(description = "信号日 YYYYMMDD")
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
	 * 命中模板
	 */
	@Schema(description = "命中模板")
	private String pattern;

	/**
	 * 规则打分
	 */
	@Schema(description = "规则打分")
	private BigDecimal screenScore;

	/**
	 * LLM加权分
	 */
	@Schema(description = "LLM加权分")
	private BigDecimal llmScore;

	/**
	 * agent/rules_fallback（A/B对比维度）
	 */
	@Schema(description = "决策模式 agent/rules_fallback")
	private String decisionMode;

	/**
	 * 置信度
	 */
	@Schema(description = "置信度")
	private Integer confidence;

	/**
	 * 入场价（信号日次一交易日开盘价）
	 */
	@Schema(description = "入场价（次日开盘价）")
	private BigDecimal entryPrice;

	/**
	 * 未入场原因 gap_up/gap_down/suspended
	 */
	@Schema(description = "未入场原因")
	private String entrySkipped;

	/**
	 * 入场日收盘收益（gross）
	 */
	@Schema(description = "入场日收盘收益")
	private BigDecimal fwd1d;

	/**
	 * 入场后第3个交易日收盘收益（gross）
	 */
	@Schema(description = "第3日收盘收益")
	private BigDecimal fwd3d;

	/**
	 * 入场后第5个交易日收盘收益（gross）
	 */
	@Schema(description = "第5日收盘收益")
	private BigDecimal fwd5d;

	/**
	 * 持有期内最高收盘收益（gross）
	 */
	@Schema(description = "持有期内最高收盘收益")
	private BigDecimal bestRet;

}
