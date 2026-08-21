package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockAgentAnalysisEntity;

/**
 * <p>
 * LLM逐Agent分析结果 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockAgentAnalysisService extends IService<StockAgentAnalysisEntity> {

	/**
	 * 执行 LLM 分析（基准日自动取候选池最新信号日）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockAgentAnalysisService
	 *   methodName   = analyze
	 * </pre>
	 * @return 处理的股票数
	 */
	int analyze();

	/**
	 * 执行 LLM 分析（指定基准日，对当日候选池 entry_ok 候选逐只分析）
	 * <p>
	 * 流程：Dify Workflow 调用（失败自动规则降级，流水线不中断）
	 * -> 逐 Agent 结果落库 -> 加权聚合（技术0.30/板块0.15/资金流0.15/龙虎榜0.10/新闻0.10/政策0.10）
	 * -> 与规则分合并 Gate（LLM 反向一票否决、同向提升置信度）
	 * -> 回写候选池（llm_score/confidence/action/reasons/agent_summary/decision_mode）。
	 * @param tradeDate 基准日 YYYYMMDD
	 * @return 处理的股票数
	 */
	int analyze(String tradeDate);

}
