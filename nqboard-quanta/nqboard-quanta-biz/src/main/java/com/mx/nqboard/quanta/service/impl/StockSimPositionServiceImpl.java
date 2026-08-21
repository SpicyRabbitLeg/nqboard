package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockPositionDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockSimPositionEntity;
import com.mx.nqboard.quanta.mapper.StockCandidateMapper;
import com.mx.nqboard.quanta.mapper.StockDailyMapper;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.mapper.StockSimPositionMapper;
import com.mx.nqboard.quanta.screen.ExitEngine;
import com.mx.nqboard.quanta.service.StockPositionDailyService;
import com.mx.nqboard.quanta.service.StockSimPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 模拟持仓 服务实现类（T+1 交易闭环）
 * </p>
 * <p>
 * 与回测共用 ExitEngine 六规则，成交假设完全一致：信号日收盘出信号 ->
 * 次日开盘价成交（H7 跳空 >+5%/-3% 放弃）-> 收盘触发离场、次日开盘卖出。
 * 每个交易日收盘数据就绪后调用 {@link #trackPositions(String)} 完成当日处理。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSimPositionServiceImpl extends ServiceImpl<StockSimPositionMapper, StockSimPositionEntity>
		implements StockSimPositionService {

	private static final String BENCHMARK_INDEX = "sh000300";

	/**
	 * 交易日历回看天数（覆盖最大持有期+缓冲）
	 */
	private static final int CALENDAR_LOOKBACK = 60;

	private final StockSimPositionMapper stockSimPositionMapper;

	private final StockCandidateMapper stockCandidateMapper;

	private final StockDailyMapper stockDailyMapper;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	private final StockPositionDailyService stockPositionDailyService;

	/**
	 * 模拟账户初始资金（yml quanta.sim.capital）
	 */
	@Value("${quanta.sim.capital:100000}")
	private double capital;

	/**
	 * 最大并发持仓数
	 */
	@Value("${quanta.sim.max-positions:4}")
	private int maxPositions;

	/**
	 * 单仓位最大资金占比
	 */
	@Value("${quanta.sim.max-position-pct:0.25}")
	private double maxPositionPct;

	/**
	 * 买入成本率（佣金）
	 */
	@Value("${quanta.sim.cost-buy:0.0003}")
	private double costBuy;

	/**
	 * 卖出成本率（佣金+印花税）
	 */
	@Value("${quanta.sim.cost-sell:0.0008}")
	private double costSell;

	@Override
	public Long createPendingBuy(Long candidateId) {
		StockCandidateEntity candidate = stockCandidateMapper.selectById(candidateId);
		if (candidate == null) {
			throw new IllegalArgumentException("候选记录不存在: " + candidateId);
		}
		if (!"ACTIVE".equals(candidate.getStatus())) {
			throw new IllegalStateException("候选已不在有效期内（status=" + candidate.getStatus() + "）");
		}
		// 仅允许买入最新信号日的候选（保证"次日开盘成交"语义）
		String latest = latestTradeDate();
		if (!candidate.getTradeDate().equals(latest)) {
			throw new IllegalStateException("仅可买入最新信号日(" + latest + ")的候选，该候选信号日为 "
					+ candidate.getTradeDate());
		}
		// 同票去重
		Long dup = countActive(candidate.getTsCode());
		if (dup != null && dup > 0) {
			throw new IllegalStateException("该股票已有在途/持仓记录");
		}
		// 持仓数约束
		Long activeTotal = stockSimPositionMapper.selectCount(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.in(StockSimPositionEntity::getStatus, "PENDING_BUY", "HOLDING", "PENDING_SELL"));
		if (activeTotal != null && activeTotal >= maxPositions) {
			throw new IllegalStateException("已达最大并发持仓数 " + maxPositions);
		}

		StockSimPositionEntity position = new StockSimPositionEntity();
		position.setCandidateId(candidateId);
		position.setTsCode(candidate.getTsCode());
		// PENDING_BUY 期间 buy_date 记录信号日，成交时更新为实际买入日
		position.setBuyDate(candidate.getTradeDate());
		position.setMaxHoldDays(ExitEngine.MAX_HOLD_DAYS);
		position.setStatus("PENDING_BUY");
		save(position);
		log.info("创建模拟买入委托: candidateId={}, tsCode={}, 信号日={}，待次日开盘成交", candidateId, candidate.getTsCode(),
				candidate.getTradeDate());
		return position.getId();
	}

	@Override
	public int trackPositions() {
		String latest = latestTradeDate();
		if (StrUtil.isBlank(latest)) {
			throw new IllegalStateException("指数日线无数据，请先执行 index_daily 同步步骤");
		}
		return trackPositions(latest);
	}

	@Override
	public int trackPositions(String tradeDate) {
		List<String> tradingDays = recentTradingDays(tradeDate, CALENDAR_LOOKBACK);
		int processed = 0;

		// --- 1. PENDING_SELL 开盘成交（昨收盘触发，今日开盘卖出；停牌顺延） ---
		List<StockSimPositionEntity> pendingSells = list(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getStatus, "PENDING_SELL"));
		for (StockSimPositionEntity pos : pendingSells) {
			StockDailyEntity bar = bar(pos.getTsCode(), tradeDate);
			if (bar == null || bar.getOpen() == null) {
				continue;
			}
			sellAtOpen(pos, bar.getOpen().doubleValue(), tradeDate, tradingDays);
			writeDaily(pos, tradeDate, bar.getOpen().doubleValue(), "SELL",
					"卖出成交(" + pos.getExitReason() + ")", true);
			processed++;
		}

		// --- 2. PENDING_BUY 开盘成交（H7 跳空保护 + 仓位计算） ---
		List<StockSimPositionEntity> pendingBuys = list(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getStatus, "PENDING_BUY")
				.lt(StockSimPositionEntity::getBuyDate, tradeDate));
		for (StockSimPositionEntity pos : pendingBuys) {
			StockDailyEntity bar = bar(pos.getTsCode(), tradeDate);
			if (bar == null || bar.getOpen() == null || bar.getOpen().doubleValue() <= 0) {
				continue; // 停牌顺延
			}
			double open = bar.getOpen().doubleValue();
			Double preClose = bar.getPreClose() != null ? bar.getPreClose().doubleValue() : null;
			if (preClose != null && ExitEngine.entryGapBlocked(preClose, open)) {
				pos.setStatus("CANCELLED");
				pos.setExitReason(open > preClose ? "gap_up" : "gap_down");
				pos.setExitDate(tradeDate);
				updateById(pos);
				log.info("模拟买入放弃: {} 开盘跳空({}->{}), reason={}", pos.getTsCode(), preClose, open,
						pos.getExitReason());
				continue;
			}
			double maxBuyValue = capital * maxPositionPct;
			int qty = (int) Math.floor(maxBuyValue / open / 100) * 100;
			if (qty < 100) {
				pos.setStatus("CANCELLED");
				pos.setExitReason("qty_zero");
				pos.setExitDate(tradeDate);
				updateById(pos);
				continue;
			}
			double cost = qty * open * (1 + costBuy);
			pos.setBuyDate(tradeDate);
			pos.setBuyPrice(BigDecimal.valueOf(open).setScale(3, RoundingMode.HALF_UP));
			pos.setQty(qty);
			pos.setCost(BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP));
			pos.setStopPrice(BigDecimal.valueOf(open * (1 - ExitEngine.STOP_LOSS_PCT)).setScale(2, RoundingMode.HALF_UP));
			pos.setTargetPrice(BigDecimal.valueOf(open * (1 + ExitEngine.TAKE_PROFIT_PCT)).setScale(2, RoundingMode.HALF_UP));
			pos.setStatus("HOLDING");
			updateById(pos);
			log.info("模拟买入成交: {} @{} x{}", pos.getTsCode(), open, qty);
			processed++;
		}

		// --- 3. HOLDING 开盘急杀检查（规则⑥，第2日起生效） ---
		List<StockSimPositionEntity> holdings = list(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getStatus, "HOLDING"));
		for (StockSimPositionEntity pos : holdings) {
			StockDailyEntity bar = bar(pos.getTsCode(), tradeDate);
			if (bar == null || bar.getOpen() == null) {
				continue;
			}
			double open = bar.getOpen().doubleValue();
			double entry = pos.getBuyPrice().doubleValue();
			int heldDays = tradingDaysBetween(pos.getBuyDate(), tradeDate, tradingDays);
			if (heldDays >= 1 && ExitEngine.isGapStop(entry, open)) {
				sellAtOpen(pos, open, tradeDate, tradingDays);
				writeDaily(pos, tradeDate, open, "SELL", "开盘急杀止损", true);
				processed++;
			}
		}

		// --- 4. HOLDING 收盘离场评估（规则①-⑤，T+1 次日开盘成交） ---
		holdings = list(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getStatus, "HOLDING"));
		for (StockSimPositionEntity pos : holdings) {
			StockDailyEntity bar = bar(pos.getTsCode(), tradeDate);
			if (bar == null || bar.getClose() == null) {
				continue;
			}
			double close = bar.getClose().doubleValue();
			double entry = pos.getBuyPrice().doubleValue();
			int heldDays = tradingDaysBetween(pos.getBuyDate(), tradeDate, tradingDays);
			double peakClose = peakClose(pos.getId(), close);
			String reason = ExitEngine.evaluateClose(entry, heldDays, peakClose, close);
			if (reason != null) {
				pos.setStatus("PENDING_SELL");
				pos.setExitReason(reason);
				pos.setHeldDays(heldDays);
				updateById(pos);
				writeDaily(pos, tradeDate, close, "PENDING_SELL", "明日开盘卖出(" + reason + ")", false);
				log.info("离场信号: {} 持有{}天 收盘{} reason={}", pos.getTsCode(), heldDays, close, reason);
			}
			else {
				int daysLeft = ExitEngine.MAX_HOLD_DAYS - heldDays;
				String note = String.format("持有 %d/%d 天，距止损 %.2f / 距止盈 %.2f，剩余 %d 天", heldDays,
						ExitEngine.MAX_HOLD_DAYS, pos.getStopPrice().doubleValue(), pos.getTargetPrice().doubleValue(),
						daysLeft);
				writeDaily(pos, tradeDate, close, "HOLD", note, false);
			}
			processed++;
		}

		log.info("持仓跟踪完成: 交易日={}, 处理 {} 条", tradeDate, processed);
		return processed;
	}

	// ==================== 内部方法 ====================

	/**
	 * 开盘卖出成交（回填离场字段）
	 */
	private void sellAtOpen(StockSimPositionEntity pos, double exitPrice, String tradeDate, List<String> tradingDays) {
		double proceeds = pos.getQty() * exitPrice * (1 - costSell);
		double cost = pos.getCost().doubleValue();
		pos.setStatus("EXITED");
		pos.setExitDate(tradeDate);
		pos.setExitPrice(BigDecimal.valueOf(exitPrice).setScale(3, RoundingMode.HALF_UP));
		pos.setProceeds(BigDecimal.valueOf(proceeds).setScale(2, RoundingMode.HALF_UP));
		pos.setPnl(BigDecimal.valueOf(proceeds - cost).setScale(2, RoundingMode.HALF_UP));
		pos.setRet(BigDecimal.valueOf(proceeds / cost - 1).setScale(4, RoundingMode.HALF_UP));
		pos.setHeldDays(tradingDaysBetween(pos.getBuyDate(), tradeDate, tradingDays));
		updateById(pos);
		log.info("模拟卖出成交: {} @{} reason={} ret={}", pos.getTsCode(), exitPrice, pos.getExitReason(),
				pos.getRet());
	}

	/**
	 * 写逐日盯市记录（按 position_id + trade_date 幂等 upsert）
	 */
	private void writeDaily(StockSimPositionEntity pos, String tradeDate, double price, String action,
			String reason, boolean exited) {
		double qty = pos.getQty();
		double cost = pos.getCost().doubleValue();
		// 当日盈亏：与前一盯市日收盘比较
		StockPositionDailyEntity prev = stockPositionDailyService.getOne(
				Wrappers.<StockPositionDailyEntity>lambdaQuery()
						.eq(StockPositionDailyEntity::getPositionId, pos.getId())
						.orderByDesc(StockPositionDailyEntity::getTradeDate)
						.last("limit 1"));
		double prevClose = prev != null && prev.getClose() != null ? prev.getClose().doubleValue()
				: pos.getBuyPrice().doubleValue();
		double dayPnl = qty * (price - prevClose);
		double cumPnl = exited && pos.getPnl() != null ? pos.getPnl().doubleValue() : qty * price - cost;
		double cumRet = exited && pos.getRet() != null ? pos.getRet().doubleValue() : qty * price / cost - 1;

		StockPositionDailyEntity daily = stockPositionDailyService.getOne(
				Wrappers.<StockPositionDailyEntity>lambdaQuery()
						.eq(StockPositionDailyEntity::getPositionId, pos.getId())
						.eq(StockPositionDailyEntity::getTradeDate, tradeDate));
		if (daily == null) {
			daily = new StockPositionDailyEntity();
		}
		daily.setPositionId(pos.getId());
		daily.setTradeDate(tradeDate);
		daily.setClose(BigDecimal.valueOf(price).setScale(3, RoundingMode.HALF_UP));
		daily.setDayPnl(BigDecimal.valueOf(dayPnl).setScale(2, RoundingMode.HALF_UP));
		daily.setCumPnl(BigDecimal.valueOf(cumPnl).setScale(2, RoundingMode.HALF_UP));
		daily.setCumRet(BigDecimal.valueOf(cumRet).setScale(4, RoundingMode.HALF_UP));
		daily.setAction(action);
		daily.setActionReason(reason);
		stockPositionDailyService.saveOrUpdate(daily);
	}

	/**
	 * 持有期间最高收盘价（含当日，保本止损判定用）
	 */
	private double peakClose(Long positionId, double todayClose) {
		Double max = stockPositionDailyService.list(Wrappers.<StockPositionDailyEntity>lambdaQuery()
				.select(StockPositionDailyEntity::getClose)
				.eq(StockPositionDailyEntity::getPositionId, positionId))
				.stream()
				.map(StockPositionDailyEntity::getClose)
				.filter(java.util.Objects::nonNull)
				.map(BigDecimal::doubleValue)
				.max(Double::compareTo)
				.orElse(todayClose);
		return Math.max(max, todayClose);
	}

	/**
	 * 已持有交易日数（买入日=0）：交易日历区间计数-1
	 */
	private int tradingDaysBetween(String buyDate, String tradeDate, List<String> tradingDays) {
		int buyIdx = tradingDays.indexOf(buyDate);
		int curIdx = tradingDays.indexOf(tradeDate);
		if (buyIdx < 0 || curIdx < 0) {
			// 日历窗口外退化为按存在的盯市记录数推算
			return 1;
		}
		return curIdx - buyIdx;
	}

	private Long countActive(String tsCode) {
		return stockSimPositionMapper.selectCount(Wrappers.<StockSimPositionEntity>lambdaQuery()
				.eq(StockSimPositionEntity::getTsCode, tsCode)
				.in(StockSimPositionEntity::getStatus, "PENDING_BUY", "HOLDING", "PENDING_SELL"));
	}

	private StockDailyEntity bar(String tsCode, String tradeDate) {
		return stockDailyMapper.selectOne(Wrappers.<StockDailyEntity>lambdaQuery()
				.eq(StockDailyEntity::getTsCode, tsCode)
				.eq(StockDailyEntity::getTradeDate, tradeDate)
				.last("limit 1"));
	}

	private String latestTradeDate() {
		StockIndexDailyEntity latest = stockIndexDailyMapper.selectOne(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit 1"));
		return latest != null ? latest.getTradeDate() : null;
	}

	/**
	 * 近 N 个交易日（含 tradeDate，升序）
	 */
	private List<String> recentTradingDays(String tradeDate, int limit) {
		List<String> days = stockIndexDailyMapper.selectList(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
				.le(StockIndexDailyEntity::getTradeDate, tradeDate)
				.orderByDesc(StockIndexDailyEntity::getTradeDate)
				.last("limit " + limit))
				.stream()
				.map(StockIndexDailyEntity::getTradeDate)
				.collect(Collectors.toList());
		Collections.reverse(days);
		return days;
	}

}
