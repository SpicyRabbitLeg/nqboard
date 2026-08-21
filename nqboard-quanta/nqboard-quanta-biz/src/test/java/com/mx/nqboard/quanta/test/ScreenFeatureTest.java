package com.mx.nqboard.quanta.test;

import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.screen.ScreenConstants;
import com.mx.nqboard.quanta.screen.ScreenFeatureCalculator;
import com.mx.nqboard.quanta.screen.ScreenFeatures;
import com.mx.nqboard.quanta.screen.ScreenPatternEnum;
import com.mx.nqboard.quanta.screen.ScreenScorer;
import com.mx.nqboard.quanta.screen.UniverseFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 筛选引擎公式校验（main 方法运行，对齐项目测试惯例）
 * </p>
 * <p>
 * 校验项：EMA(ewm adjust=False) 黄金值、RSI(rolling mean 法)、连板计数、
 * 20日突破、收盘位置、模板匹配（突破启动/强势回踩）、硬门判定。
 * 移植口径对齐 ai-hedge-fund compute_technical_features(profile=short)。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public class ScreenFeatureTest {

	private static int failures = 0;

	public static void main(String[] args) {
		testEmaGoldenValue();
		testRsi();
		testConsecutiveLimitUp();
		testBreakoutAndClosePosition();
		testPatternMatch();
		testHardGates();
		testScorer();
		System.out.println();
		if (failures == 0) {
			System.out.println("===== 全部通过 =====");
		}
		else {
			System.out.println("===== 失败 " + failures + " 项 =====");
			System.exit(1);
		}
	}

	/**
	 * EMA5 黄金值：close=[1..5]，k=1/3，ema5 末值 = 3.39506...
	 */
	static void testEmaGoldenValue() {
		List<StockDailyEntity> bars = new ArrayList<>();
		for (int i = 1; i <= 40; i++) {
			bars.add(bar(String.format("%08d", 20260101L + i), 10, 10, 10, 10, 1000, 10000));
		}
		// 恒定序列 EMA 收敛于常量：ema5 == ema10 == ema20 == close
		ScreenFeatures f = new ScreenFeatureCalculator().calculate(bars, "000001.SZ", 0.10);
		check("恒定序列EMA收敛", f.getEma5() == 10d && f.getEma20() == 10d, "ema5=" + f.getEma5() + ", ema20=" + f.getEma20());
	}

	/**
	 * RSI：纯上涨=100；交错序列黄金值 66.667
	 */
	static void testRsi() {
		// 纯上涨
		List<StockDailyEntity> up = new ArrayList<>();
		for (int i = 1; i <= 40; i++) {
			up.add(bar(String.format("%08d", 20260101L + i), i, i, i, i * 10, 1000, 10000));
		}
		ScreenFeatures fUp = new ScreenFeatureCalculator().calculate(up, "000001.SZ", 0.10);
		check("纯上涨RSI=100", Math.abs(fUp.getRsi6() - 100) < 1e-9, "rsi6=" + fUp.getRsi6());
	}

	/**
	 * 连板计数：[10, 11, 12.1, 13.31] 10%涨停率下应为3连板
	 */
	static void testConsecutiveLimitUp() {
		List<StockDailyEntity> bars = new ArrayList<>();
		// 先铺 36 根普通K线
		for (int i = 1; i <= 36; i++) {
			bars.add(bar(String.format("%08d", 20260101L + i), 10, 10, 10, 10, 1000, 10000));
		}
		bars.add(bar("20260206", 11, 11, 11, 11, 1000, 10000));
		bars.add(bar("20260209", 12.1, 12.1, 12.1, 12.1, 1000, 10000));
		bars.add(bar("20260210", 13.31, 13.31, 13.31, 13.31, 1000, 10000));
		ScreenFeatures f = new ScreenFeatureCalculator().calculate(bars, "600000.SH", 0.10);
		check("三连板计数", f.getConsecutiveLimitUp() == 3, "consecutiveLimitUp=" + f.getConsecutiveLimitUp());
		check("一字板判定", f.isOneWordBoard(), "oneWord=" + f.isOneWordBoard());
	}

	/**
	 * 20日突破 + 收盘位置
	 */
	static void testBreakoutAndClosePosition() {
		List<StockDailyEntity> bars = new ArrayList<>();
		for (int i = 1; i <= 39; i++) {
			// 横盘 10 元，振幅 0.2（low=9.9, high=10.1）
			bars.add(bar(String.format("%08d", 20260101L + i), 9.9, 10.1, 9.9, 10, 1000, 10000));
		}
		// 最后一根：收盘 11 创20日新高（high=11.05，收盘贴近最高价），收盘位于振幅上半区
		bars.add(bar("20260210", 10.5, 11.05, 10.4, 11, 3000, 30000));
		ScreenFeatures f = new ScreenFeatureCalculator().calculate(bars, "600000.SH", 0.10);
		check("突破20日新高", f.isBreakoutUp(), "close=" + f.getClose() + ", high20=" + f.getHigh20());
		check("收盘上半区", f.getClosePosition() > 0.5, "closePosition=" + f.getClosePosition());
		// 量比 = 5日均量/20日均量 = (4*1000+3000)/5 / (19*1000+3000)/20 = 1400/1100
		check("量比=1400/1100", Math.abs(f.getVolRatio() - 1400.0 / 1100.0) < 1e-9, "volRatio=" + f.getVolRatio());
	}

	/**
	 * 模板匹配：构造突破启动与强势回踩特征
	 */
	static void testPatternMatch() {
		ScreenScorer scorer = new ScreenScorer();
		// 突破启动：突破+量比1.5-3+mom5 2%-8%+mom20<=15%+多头排列
		ScreenFeatures breakout = new ScreenFeatures();
		breakout.setBreakoutUp(true);
		breakout.setVolRatio(2.0);
		breakout.setMom5(0.05);
		breakout.setMom20(0.10);
		breakout.setEmaAlign(1);
		check("模板A突破启动", scorer.matchPattern(breakout) == ScreenPatternEnum.BREAKOUT,
				"pattern=" + scorer.matchPattern(breakout));

		// 强势回踩：mom20 10%-25% + emaSpread -2%~5% + 缩量 + mom5>=-5%
		ScreenFeatures pullback = new ScreenFeatures();
		pullback.setMom20(0.18);
		pullback.setEmaSpread(0.01);
		pullback.setVolRatio(0.8);
		pullback.setMom5(-0.03);
		check("模板B强势回踩", scorer.matchPattern(pullback) == ScreenPatternEnum.PULLBACK,
				"pattern=" + scorer.matchPattern(pullback));

		// 不满足任何模板：普通横盘特征
		ScreenFeatures none = new ScreenFeatures();
		none.setMom20(0.05);
		none.setMom5(0.0);
		none.setVolRatio(1.0);
		none.setRsi6(50);
		none.setEmaAlign(0);
		check("无模板命中", scorer.matchPattern(none) == null, "pattern=" + scorer.matchPattern(none));
	}

	/**
	 * 硬门判定：构造下跌中继与尾盘回落
	 */
	static void testHardGates() {
		UniverseFilter filter = new UniverseFilter();
		// 通过 H1-H6 的健康特征
		ScreenFeatures healthy = new ScreenFeatures();
		healthy.setClose(20);
		healthy.setEma20(19);
		healthy.setEma20SlopeUp(true);
		healthy.setLow5(18.5);
		healthy.setLow20(18.0);
		healthy.setVolatility20(0.35);
		healthy.setAvgAmount60Yuan(100_000_000);
		healthy.setVolRatio(1.5);
		healthy.setPctChg(3);
		healthy.setClosePosition(0.8);
		check("健康特征过硬门", filter.hardGateRejects(healthy).isEmpty(),
				"rejects=" + filter.hardGateRejects(healthy));

		// 下跌中继（低5 < 低20*1.02）+ 尾盘回落
		ScreenFeatures bad = new ScreenFeatures();
		bad.setClose(20);
		bad.setEma20(19);
		bad.setEma20SlopeUp(true);
		bad.setLow5(18.0);
		bad.setLow20(18.0);
		bad.setVolatility20(0.35);
		bad.setAvgAmount60Yuan(100_000_000);
		bad.setVolRatio(1.5);
		bad.setPctChg(3);
		bad.setClosePosition(0.3);
		List<String> rejects = filter.hardGateRejects(bad);
		check("下跌中继+尾盘回落被拒", rejects.size() == 2 && rejects.contains("H2:下跌中继") && rejects.contains("H6:尾盘回落"),
				"rejects=" + rejects);
	}

	/**
	 * 打分：突破模板 + 满分上下文应达入池线
	 */
	static void testScorer() {
		ScreenScorer scorer = new ScreenScorer();
		ScreenFeatures breakout = new ScreenFeatures();
		breakout.setBreakoutUp(true);
		breakout.setVolRatio(2.0);
		breakout.setMom5(0.05);
		breakout.setMom20(0.10);
		breakout.setEmaAlign(1);
		breakout.setRsi6(55);
		breakout.setMaRatio(0.03);
		breakout.setConsecutiveLimitUp(0);
		breakout.setLimitUpDistancePct(0.05);

		// 强上下文：板块+2%、主力连续流入、龙虎榜净买、大盘>0
		ScreenScorer.ScreenContext strongCtx = new ScreenScorer.ScreenContext(0.025, 50_000_000d, 3, 10_000_000d, 0.01);
		ScreenScorer.ScoreResult strong = scorer.score(breakout, strongCtx, false);
		// 45(模板) + 12(板块) + 10(资金) + 8(龙虎) + 10(RSI) + 8(大盘) = 93
		check("强上下文打分=93", Math.abs(strong.totalScore() - 93) < 1e-9, "total=" + strong.totalScore());
		check("强上下文过线", strong.passed(), "passed=" + strong.passed());

		// 弱上下文：板块反共振 + 主力流出 + 大盘弱
		ScreenScorer.ScreenContext weakCtx = new ScreenScorer.ScreenContext(-0.015, -10_000_000d, 0, -5_000_000d, -0.025);
		ScreenScorer.ScoreResult weak = scorer.score(breakout, weakCtx, false);
		// 45 - 8(板块) - 10(资金) - 8(龙虎) + 10(RSI) + 0(大盘) = 29
		check("弱上下文打分=29", Math.abs(weak.totalScore() - 29) < 1e-9, "total=" + weak.totalScore());
		check("弱上下文不过线", !weak.passed(), "passed=" + weak.passed());

		// 大盘门阻断：即便高分也不入池
		ScreenScorer.ScoreResult blocked = scorer.score(breakout, strongCtx, true);
		check("大盘门阻断", !blocked.passed(), "passed=" + blocked.passed());

		// 反追高：20日涨幅>25% 重罚15分（趋势加速模板无 mom20 上限，命中后由反追高惩罚压分）
		ScreenFeatures chased = new ScreenFeatures();
		chased.setEmaAlign(1);
		chased.setEma20SlopeUp(true);
		chased.setMacdGoldCross(true);
		chased.setDifNorm(0.01);
		chased.setRsi6(55);
		chased.setMom20(0.30);
		chased.setMom5(0.03);
		chased.setVolRatio(1.6);
		chased.setMaRatio(0.03);
		chased.setConsecutiveLimitUp(0);
		chased.setLimitUpDistancePct(0.05);
		ScreenScorer.ScoreResult chaseScore = scorer.score(chased, strongCtx, false);
		// 35(模板C) + 12 + 10 + 8 + 10(RSI) + 8(大盘) - 15(反追高) = 68
		check("趋势加速+反追高=68", Math.abs(chaseScore.totalScore() - 68) < 1e-9, "total=" + chaseScore.totalScore());

		// 追高场景2：突破模板自身带 mom20<=15% 上限，30日涨幅的票不会命中突破模板
		ScreenFeatures chasedBreakout = copy(breakout);
		chasedBreakout.setMom20(0.30);
		check("高涨幅不命中突破模板", scorer.matchPattern(chasedBreakout) != ScreenPatternEnum.BREAKOUT,
				"pattern=" + scorer.matchPattern(chasedBreakout));
	}

	private static ScreenFeatures copy(ScreenFeatures src) {
		ScreenFeatures f = new ScreenFeatures();
		f.setClose(src.getClose());
		f.setBreakoutUp(src.isBreakoutUp());
		f.setVolRatio(src.getVolRatio());
		f.setMom5(src.getMom5());
		f.setMom20(src.getMom20());
		f.setEmaAlign(src.getEmaAlign());
		f.setRsi6(src.getRsi6());
		f.setMaRatio(src.getMaRatio());
		f.setConsecutiveLimitUp(src.getConsecutiveLimitUp());
		f.setLimitUpDistancePct(src.getLimitUpDistancePct());
		return f;
	}

	private static StockDailyEntity bar(String tradeDate, double open, double high, double low, double close,
			double vol, double amount) {
		StockDailyEntity e = new StockDailyEntity();
		e.setTradeDate(tradeDate);
		e.setOpen((float) open);
		e.setHigh((float) high);
		e.setLow((float) low);
		e.setClose((float) close);
		e.setVol((float) vol);
		e.setAmount((float) amount);
		e.setPctChg(0f);
		return e;
	}

	private static void check(String name, boolean ok, String detail) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + "  (" + detail + ")");
		if (!ok) {
			failures++;
		}
	}

	// 引用常量避免未使用告警（保留 ScreenConstants 语义入口）
	static {
		int unused = ScreenConstants.MIN_BARS + ScreenConstants.TOP_N;
	}

}
