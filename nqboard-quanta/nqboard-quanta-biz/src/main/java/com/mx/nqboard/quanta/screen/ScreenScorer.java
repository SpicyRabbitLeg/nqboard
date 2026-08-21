package com.mx.nqboard.quanta.screen;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 打分器：入场模板匹配（四选一）+ 质量加分（板块/资金/龙虎榜/RSI/大盘/反追高/连板）
 * </p>
 * <p>
 * 总分 = 模板基础分 + 上下文加分，截断到 [0,100]。入池线默认 65 分（yml 可调）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Component
public class ScreenScorer {

	/**
	 * 入池线（yml quanta.screen.min-score）
	 */
	@org.springframework.beans.factory.annotation.Value("${quanta.screen.min-score:" + ScreenConstants.MIN_SCORE + "}")
	private double minScore = ScreenConstants.MIN_SCORE;

	/**
	 * 上下文数据（板块共振/主力资金/龙虎榜/大盘），缺数据项自动降级为 0 分
	 */
	public record ScreenContext(Double sectorPct, Double flow3dSum, Integer flowConsecutiveInflow,
			Double dragonNetSum, double marketRet5d) {

	}

	/**
	 * 打分结果
	 */
	public record ScoreResult(ScreenPatternEnum pattern, double patternScore, double contextScore, double totalScore,
			boolean passed, List<String> reasons, Map<String, Object> metrics) {

	}

	/**
	 * 未命中模板的占位结果（pattern=null，不入池）
	 */
	public ScoreResult noPattern(Map<String, Object> metrics) {
		return new ScoreResult(null, 0, 0, 0, false, List.of("未命中入场模板"), metrics);
	}

	/**
	 * 打分主入口：模板匹配 + 上下文加分
	 * @param f 特征向量
	 * @param ctx 上下文数据
	 * @param marketBlocked H8 大盘门是否阻断（阻断时强制不入池）
	 */
	public ScoreResult score(ScreenFeatures f, ScreenContext ctx, boolean marketBlocked) {
		Map<String, Object> metrics = buildMetrics(f, ctx);
		ScreenPatternEnum pattern = matchPattern(f);
		if (pattern == null) {
			return noPattern(metrics);
		}

		List<String> reasons = new ArrayList<>();
		reasons.add("模板:" + pattern.getLabel());

		double context = 0;
		// --- 板块共振 ---
		if (ctx.sectorPct() != null) {
			if (ctx.sectorPct() >= ScreenConstants.SECTOR_RET_STRONG) {
				context += ScreenConstants.SECTOR_BONUS_STRONG;
				reasons.add("板块强共振");
			}
			else if (ctx.sectorPct() >= ScreenConstants.SECTOR_RET_MID) {
				context += ScreenConstants.SECTOR_BONUS_MID;
				reasons.add("板块共振");
			}
			else if (ctx.sectorPct() <= ScreenConstants.SECTOR_RET_WEAK) {
				context += ScreenConstants.SECTOR_PENALTY_WEAK;
			}
		}
		// --- 主力资金（3日累计净流入 + 连续流入天数） ---
		if (ctx.flow3dSum() != null) {
			if (ctx.flow3dSum() > 0 && ctx.flowConsecutiveInflow() != null && ctx.flowConsecutiveInflow() >= 2) {
				context += ScreenConstants.FLOW_BONUS;
				reasons.add("主力持续净流入");
			}
			else if (ctx.flow3dSum() < 0) {
				context += ScreenConstants.FLOW_PENALTY;
			}
		}
		// --- 龙虎榜（近5个交易日净买/净卖） ---
		if (ctx.dragonNetSum() != null && ctx.dragonNetSum() != 0) {
			if (ctx.dragonNetSum() > 0) {
				context += ScreenConstants.DRAGON_BONUS;
				reasons.add("龙虎榜净买");
			}
			else {
				context += ScreenConstants.DRAGON_PENALTY;
			}
		}
		// --- RSI 区间 ---
		double rsi = f.getRsi6();
		if (rsi > 70) {
			context -= 10;
		}
		else if (40 <= rsi && rsi <= 70) {
			context += 10;
		}
		else if (25 < rsi && rsi < 40) {
			context += 8;
		}
		// --- 大盘环境 ---
		if (ctx.marketRet5d() > 0) {
			context += 8;
		}
		else if (ctx.marketRet5d() >= -0.02) {
			context += 4;
		}
		// --- 反追高（原算法最值钱的部分，保留） ---
		if (f.getMom20() > ScreenConstants.ANTI_CHASE_MOM20) {
			context += ScreenConstants.ANTI_CHASE_MOM20_PENALTY;
			reasons.add("20日涨幅过大");
		}
		if (f.getMaRatio() > ScreenConstants.ANTI_CHASE_MA_RATIO) {
			context += ScreenConstants.ANTI_CHASE_MA_RATIO_PENALTY;
		}
		// --- 连板惩罚（≥2连板且仍封板：买不进+隔日风险） ---
		if (f.getConsecutiveLimitUp() >= ScreenConstants.CONSEC_LIMIT_UP
				&& f.getLimitUpDistancePct() < ScreenConstants.CONSEC_LIMIT_UP_DISTANCE) {
			context += ScreenConstants.CONSEC_LIMIT_PENALTY;
			reasons.add("连板惩罚");
		}

		double total = clamp(pattern.getBaseScore() + context, 0, 100);
		boolean passed = total >= minScore && !marketBlocked;
		if (marketBlocked) {
			reasons.add("H8:大盘门阻断");
		}
		return new ScoreResult(pattern, pattern.getBaseScore(), context, total, passed, reasons, metrics);
	}

	/**
	 * 入场模板匹配（多模板命中取基础分最高者）
	 */
	public ScreenPatternEnum matchPattern(ScreenFeatures f) {
		ScreenPatternEnum best = null;
		if (isBreakout(f) && (best == null || ScreenPatternEnum.BREAKOUT.getBaseScore() > best.getBaseScore())) {
			best = ScreenPatternEnum.BREAKOUT;
		}
		if (isPullback(f) && (best == null || ScreenPatternEnum.PULLBACK.getBaseScore() > best.getBaseScore())) {
			best = ScreenPatternEnum.PULLBACK;
		}
		if (isTrendAccel(f) && (best == null || ScreenPatternEnum.TREND_ACCEL.getBaseScore() > best.getBaseScore())) {
			best = ScreenPatternEnum.TREND_ACCEL;
		}
		if (isOversold(f) && (best == null || ScreenPatternEnum.OVERSOLD.getBaseScore() > best.getBaseScore())) {
			best = ScreenPatternEnum.OVERSOLD;
		}
		return best;
	}

	/**
	 * 模板A 突破启动：突破20日新高 + 温和放量 + 启动初期动量 + 首次突破 + 多头排列
	 */
	private boolean isBreakout(ScreenFeatures f) {
		return f.isBreakoutUp()
				&& f.getVolRatio() >= ScreenConstants.BREAKOUT_VOL_RATIO_MIN
				&& f.getVolRatio() <= ScreenConstants.BREAKOUT_VOL_RATIO_MAX
				&& f.getMom5() >= ScreenConstants.BREAKOUT_MOM5_MIN
				&& f.getMom5() <= ScreenConstants.BREAKOUT_MOM5_MAX
				&& f.getMom20() <= ScreenConstants.BREAKOUT_MOM20_MAX
				&& f.getEmaAlign() == 1;
	}

	/**
	 * 模板B 强势回踩低吸：前期强势 + 回踩EMA20附近未破位 + 缩量 + 回调非崩塌
	 */
	private boolean isPullback(ScreenFeatures f) {
		return f.getMom20() > ScreenConstants.PULLBACK_MOM20_MIN
				&& f.getMom20() <= ScreenConstants.PULLBACK_MOM20_MAX
				&& f.getEmaSpread() >= ScreenConstants.PULLBACK_EMA_SPREAD_MIN
				&& f.getEmaSpread() <= ScreenConstants.PULLBACK_EMA_SPREAD_MAX
				&& f.getVolRatio() <= ScreenConstants.PULLBACK_VOL_RATIO_MAX
				&& f.getMom5() >= ScreenConstants.PULLBACK_MOM5_MIN;
	}

	/**
	 * 模板C 趋势加速：多头排列 + EMA20上行 + 0轴附近MACD金叉 + RSI有力不热
	 */
	private boolean isTrendAccel(ScreenFeatures f) {
		return f.getEmaAlign() == 1
				&& f.isEma20SlopeUp()
				&& f.isMacdGoldCross()
				&& Math.abs(f.getDifNorm()) <= ScreenConstants.TREND_DIF_NORM_MAX
				&& f.getRsi6() >= ScreenConstants.TREND_RSI_MIN
				&& f.getRsi6() <= ScreenConstants.TREND_RSI_MAX;
	}

	/**
	 * 模板D 超跌反转：RSI超卖或深跌 + 当日收阳且收盘在上半区 + 当日放量确认
	 */
	private boolean isOversold(ScreenFeatures f) {
		boolean oversold = f.getRsi6() <= ScreenConstants.OVERSOLD_RSI || f.getMom5() <= ScreenConstants.OVERSOLD_MOM5;
		return oversold
				&& f.getPctChg() > 0
				&& f.getClosePosition() >= ScreenConstants.H6_MIN_CLOSE_POSITION
				&& f.getVolRatioToday() >= ScreenConstants.OVERSOLD_VOL_RATIO_MIN;
	}

	private Map<String, Object> buildMetrics(ScreenFeatures f, ScreenContext ctx) {
		Map<String, Object> metrics = new LinkedHashMap<>();
		metrics.put("close", round(f.getClose()));
		metrics.put("pctChg", round(f.getPctChg()));
		metrics.put("mom5", round(f.getMom5()));
		metrics.put("mom20", round(f.getMom20()));
		metrics.put("volRatio", round(f.getVolRatio()));
		metrics.put("volRatioToday", round(f.getVolRatioToday()));
		metrics.put("rsi6", round(f.getRsi6()));
		metrics.put("maRatio", round(f.getMaRatio()));
		metrics.put("emaAlign", f.getEmaAlign());
		metrics.put("emaSpread", round(f.getEmaSpread()));
		metrics.put("macdGoldCross", f.isMacdGoldCross());
		metrics.put("breakoutUp", f.isBreakoutUp());
		metrics.put("closePosition", round(f.getClosePosition()));
		metrics.put("volatility20", round(f.getVolatility20()));
		metrics.put("avgAmount60Wan", round(f.getAvgAmount60Yuan() / 10000));
		metrics.put("consecutiveLimitUp", f.getConsecutiveLimitUp());
		metrics.put("sectorPct", ctx.sectorPct() != null ? round(ctx.sectorPct()) : null);
		metrics.put("flow3dWan", ctx.flow3dSum() != null ? round(ctx.flow3dSum() / 10000) : null);
		metrics.put("dragonNetWan", ctx.dragonNetSum() != null ? round(ctx.dragonNetSum() / 10000) : null);
		metrics.put("marketRet5d", round(ctx.marketRet5d()));
		return metrics;
	}

	private double round(double v) {
		return Math.round(v * 10000) / 10000.0;
	}

	private double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

}
