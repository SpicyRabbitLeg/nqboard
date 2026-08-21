package com.mx.nqboard.quanta.dify;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * Dify Workflow 分析结果（End 节点输出契约的解析模型）
 * </p>
 * <p>
 * Workflow 输出契约（End 节点输出变量）：
 * <pre>
 * {
 *   "action": "entry_ok | watch | avoid",
 *   "confidence": 78,                    // 0-100
 *   "weighted_score": 0.42,              // -1 ~ 1
 *   "agents": [
 *     {"key": "dragon_tiger", "signal": "bullish|bearish|neutral|n/a",
 *      "confidence": 75, "reasoning": "5日内2次上榜净买1.2亿，游资接力"}
 *   ],
 *   "reasons": ["技术面偏多", "主力净流入"],
 *   "risk_flags": []
 * }
 * </pre>
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
public class DifyAnalysisResult {

	/**
	 * entry_ok/watch/avoid
	 */
	private String action;

	/**
	 * 综合置信度 0-100
	 */
	private Integer confidence;

	/**
	 * LLM 加权分 -1~1
	 */
	private Double weightedScore;

	/**
	 * 逐 Agent 结论
	 */
	private List<AgentResult> agents;

	/**
	 * 看多理由标签
	 */
	private List<String> reasons;

	/**
	 * 风险标记
	 */
	private List<String> riskFlags;

	/**
	 * Dify 返回的 token 用量
	 */
	private Integer totalTokens;

	/**
	 * Dify 返回的耗时（秒）
	 */
	private Double elapsedSeconds;

	/**
	 * 原始输出全文（落库调试用）
	 */
	private String rawOutput;

	@Data
	public static class AgentResult {

		/**
		 * 分析师标识 technical/sector/money_flow/dragon_tiger/news/policy
		 */
		private String key;

		/**
		 * bullish/bearish/neutral/n/a
		 */
		private String signal;

		/**
		 * 置信度 0-100
		 */
		private Integer confidence;

		/**
		 * 推理摘要
		 */
		private String reasoning;

	}

}
