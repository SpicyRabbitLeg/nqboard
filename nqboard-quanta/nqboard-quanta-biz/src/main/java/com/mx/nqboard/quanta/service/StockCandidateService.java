package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;

/**
 * <p>
 * 候选股票池 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockCandidateService extends IService<StockCandidateEntity> {

	/**
	 * 刷新候选池（信号日自动取指数日线最新交易日）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockCandidateService
	 *   methodName   = refreshCandidates
	 * </pre>
	 * @return 入池股票数
	 */
	int refreshCandidates();

	/**
	 * 刷新候选池（指定信号日）
	 * <p>
	 * 流程：取筛选结果 TopN（通过入池线）-> Gate 硬门
	 * （H9 解禁门：7日内解禁占总股本≥5% 拒绝；H10 冷却门：近期止损出局 10 个交易日内不再入选）
	 * -> upsert 候选池 -> 过期旧候选（票龄超 5 交易日未买入 -> EXPIRED）。
	 * P5 接入 Dify 后此处叠加 LLM 分析结果。
	 * @param tradeDate 信号日 YYYYMMDD
	 * @return 入池股票数
	 */
	int refreshCandidates(String tradeDate);

}
