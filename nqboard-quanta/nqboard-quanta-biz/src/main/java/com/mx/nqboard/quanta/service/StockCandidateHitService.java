package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockCandidateHitEntity;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 候选池信号命中率追踪 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockCandidateHitService extends IService<StockCandidateHitEntity> {

	/**
	 * 刷新命中率追踪（自动取最新交易日，回看近 N 个信号日）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockCandidateHitService
	 *   methodName   = refreshHits
	 * </pre>
	 * @return 处理（落库）的候选数
	 */
	int refreshHits();

	/**
	 * 刷新命中率追踪：对回看窗口内每个信号日的 entry_ok 候选，
	 * 计算次日开盘入场口径的前向收益（fwd_1d/3d/5d/best_ret），
	 * 幂等 upsert（前向数据未走完的日期部分填充，后续刷新补全）。
	 * @param tradeDate 最新交易日 YYYYMMDD（用于定位回看窗口）
	 * @return 处理（落库）的候选数
	 */
	int refreshHits(String tradeDate);

	/**
	 * 查询指定信号日的候选命中明细
	 * @param tradeDate 信号日 YYYYMMDD
	 * @return 命中明细列表
	 */
	List<StockCandidateHitEntity> daily(String tradeDate);

	/**
	 * 命中率汇总（近 N 个信号日）
	 * <p>
	 * 聚合维度：整体（3日胜率/平均收益）、按模板、按决策模式（LLM vs 规则 A/B）、
	 * 按分数区间（入池线校准）。胜率口径：fwd_3d > 0。
	 * @param days 回看信号日数（默认30）
	 * @return 汇总 JSON 结构
	 */
	Map<String, Object> summary(int days);

}
