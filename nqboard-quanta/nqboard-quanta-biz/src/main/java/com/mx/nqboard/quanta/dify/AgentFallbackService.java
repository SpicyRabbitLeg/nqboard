package com.mx.nqboard.quanta.dify;

import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.mapper.StockMoneyFlowMapper;
import com.mx.nqboard.quanta.mapper.StockTopListMapper;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * <p>
 * 规则降级分析（Dify 不可用时的兜底 Agent 信号）
 * </p>
 * <p>
 * 规则逻辑移植自 ai-hedge-fund 各 agent 的确定性分支：
 * 龙虎榜（净买/净卖）、主力资金（3日累计+连续性）、板块（共振/反共振）、
 * 技术面（已通过筛选漏斗 -> 偏多）。新闻/政策无关键词引擎，降级为 neutral（诚实降级，不假装有观点）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentFallbackService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockTopListMapper stockTopListMapper;

	private final StockMoneyFlowMapper stockMoneyFlowMapper;

	private final StockIndustryDailyMapper stockIndustryDailyMapper;

	/**
	 * 生成规则降级版的全部 Agent 信号
	 * @param tsCode TS股票代码
	 * @param tradeDate 基准日
	 * @param screenPassed 是否通过规则筛选（技术面信号来源）
	 */
	public List<DifyAnalysisResult.AgentResult> fallbackAgents(String tsCode, String tradeDate,
			boolean screenPassed) {
		List<DifyAnalysisResult.AgentResult> agents = new ArrayList<>();
		agents.add(technicalAgent(screenPassed));
		agents.add(dragonAgent(tsCode, tradeDate));
		agents.add(moneyFlowAgent(tsCode, tradeDate));
		agents.add(sectorAgent(tsCode, tradeDate));
		agents.add(neutralAgent("news"));
		agents.add(neutralAgent("policy"));
		return agents;
	}

	private DifyAnalysisResult.AgentResult technicalAgent(boolean screenPassed) {
		DifyAnalysisResult.AgentResult a = agent("technical");
		if (screenPassed) {
			a.setSignal("bullish");
			a.setConfidence(70);
			a.setReasoning("通过三层筛选（硬门+模板+加分），技术面偏多（规则降级）");
		}
		else {
			a.setSignal("neutral");
			a.setConfidence(40);
			a.setReasoning("未通过规则筛选（规则降级）");
		}
		return a;
	}

	/**
	 * 龙虎榜：窗口净买 -> bullish；净卖 -> bearish；无上榜 -> neutral
	 */
	private DifyAnalysisResult.AgentResult dragonAgent(String tsCode, String tradeDate) {
		DifyAnalysisResult.AgentResult a = agent("dragon_tiger");
		String start = LocalDate.parse(tradeDate.length() == 8
				? tradeDate.substring(0, 4) + "-" + tradeDate.substring(4, 6) + "-" + tradeDate.substring(6, 8)
				: tradeDate).minusDays(14).format(BASIC_DATE);
		List<StockTopListEntity> rows = stockTopListMapper.selectList(
				Wrappers.<StockTopListEntity>lambdaQuery()
						.eq(StockTopListEntity::getTsCode, tsCode)
						.ge(StockTopListEntity::getTradeDate, start)
						.le(StockTopListEntity::getTradeDate, tradeDate)
						.isNotNull(StockTopListEntity::getNetAmount));
		if (rows.isEmpty()) {
			a.setSignal("neutral");
			a.setConfidence(40);
			a.setReasoning("近5个交易日无上榜记录（规则降级）");
			return a;
		}
		double netSum = rows.stream().mapToDouble(r -> r.getNetAmount().doubleValue()).sum();
		if (netSum > 0) {
			a.setSignal("bullish");
			a.setConfidence(70);
			a.setReasoning(String.format("近5个交易日龙虎榜净买 %.0f 万元（规则降级）", netSum / 10000));
		}
		else {
			a.setSignal("bearish");
			a.setConfidence(65);
			a.setReasoning(String.format("近5个交易日龙虎榜净卖 %.0f 万元（规则降级）", -netSum / 10000));
		}
		return a;
	}

	/**
	 * 主力资金：3日累计净流入且连续>=2日 -> bullish；净流出 -> bearish
	 */
	private DifyAnalysisResult.AgentResult moneyFlowAgent(String tsCode, String tradeDate) {
		DifyAnalysisResult.AgentResult a = agent("money_flow");
		List<StockMoneyFlowEntity> rows = stockMoneyFlowMapper.selectList(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.eq(StockMoneyFlowEntity::getTsCode, tsCode)
						.le(StockMoneyFlowEntity::getTradeDate, tradeDate)
						.isNotNull(StockMoneyFlowEntity::getMainNetInflow)
						.orderByDesc(StockMoneyFlowEntity::getTradeDate)
						.last("limit 3"));
		if (rows.isEmpty()) {
			a.setSignal("n/a");
			a.setConfidence(0);
			a.setReasoning("无资金流数据（规则降级）");
			return a;
		}
		double sum = rows.stream().mapToDouble(r -> r.getMainNetInflow().doubleValue()).sum();
		long inflowDays = rows.stream().takeWhile(r -> r.getMainNetInflow().doubleValue() > 0).count();
		if (sum > 0 && inflowDays >= 2) {
			a.setSignal("bullish");
			a.setConfidence(70);
			a.setReasoning(String.format("主力连续 %d 日净流入，3日累计 %.0f 万元（规则降级）", inflowDays, sum / 10000));
		}
		else if (sum < 0) {
			a.setSignal("bearish");
			a.setConfidence(65);
			a.setReasoning(String.format("主力3日累计净流出 %.0f 万元（规则降级）", -sum / 10000));
		}
		else {
			a.setSignal("neutral");
			a.setConfidence(45);
			a.setReasoning("主力资金流向不明确（规则降级）");
		}
		return a;
	}

	/**
	 * 板块：当日行业涨幅 >=2% bullish；<=-1% bearish
	 */
	private DifyAnalysisResult.AgentResult sectorAgent(String tsCode, String tradeDate) {
		DifyAnalysisResult.AgentResult a = agent("sector");
		// 行业名取资金流最新快照
		StockMoneyFlowEntity latestFlow = stockMoneyFlowMapper.selectOne(
				Wrappers.<StockMoneyFlowEntity>lambdaQuery()
						.eq(StockMoneyFlowEntity::getTsCode, tsCode)
						.isNotNull(StockMoneyFlowEntity::getIndustryName)
						.orderByDesc(StockMoneyFlowEntity::getTradeDate)
						.last("limit 1"));
		if (latestFlow == null) {
			a.setSignal("n/a");
			a.setConfidence(0);
			a.setReasoning("无行业归属数据（规则降级）");
			return a;
		}
		TreeMap<String, Double> closes = new TreeMap<>();
		stockIndustryDailyMapper.selectList(Wrappers.<StockIndustryDailyEntity>lambdaQuery()
				.eq(StockIndustryDailyEntity::getBoardName, latestFlow.getIndustryName())
				.le(StockIndustryDailyEntity::getTradeDate, tradeDate)
				.isNotNull(StockIndustryDailyEntity::getClose)
				.orderByDesc(StockIndustryDailyEntity::getTradeDate)
				.last("limit 2"))
				.forEach(r -> closes.put(r.getTradeDate(), r.getClose().doubleValue()));
		if (closes.size() < 2) {
			a.setSignal("n/a");
			a.setConfidence(0);
			a.setReasoning("行业板块行情数据不足（规则降级）");
			return a;
		}
		String latestDate = closes.lastKey();
		double pct = closes.get(latestDate) / closes.lowerEntry(latestDate).getValue() - 1;
		if (pct >= 0.02) {
			a.setSignal("bullish");
			a.setConfidence(70);
			a.setReasoning(String.format("行业「%s」当日涨幅 %.1f%%，强共振（规则降级）", latestFlow.getIndustryName(), pct * 100));
		}
		else if (pct <= -0.01) {
			a.setSignal("bearish");
			a.setConfidence(60);
			a.setReasoning(String.format("行业「%s」当日跌幅 %.1f%%，反共振（规则降级）", latestFlow.getIndustryName(), pct * 100));
		}
		else {
			a.setSignal("neutral");
			a.setConfidence(45);
			a.setReasoning(String.format("行业「%s」当日涨跌幅 %.1f%%（规则降级）", latestFlow.getIndustryName(), pct * 100));
		}
		return a;
	}

	private DifyAnalysisResult.AgentResult neutralAgent(String key) {
		DifyAnalysisResult.AgentResult a = agent(key);
		a.setSignal("neutral");
		a.setConfidence(40);
		a.setReasoning("规则降级模式无新闻/政策分析能力，信号中性");
		return a;
	}

	private DifyAnalysisResult.AgentResult agent(String key) {
		DifyAnalysisResult.AgentResult a = new DifyAnalysisResult.AgentResult();
		a.setKey(key);
		return a;
	}

}
