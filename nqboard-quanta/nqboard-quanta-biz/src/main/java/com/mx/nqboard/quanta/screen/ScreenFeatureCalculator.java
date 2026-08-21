package com.mx.nqboard.quanta.screen;

import cn.hutool.core.collection.CollUtil;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 短线特征计算器（纯计算，无数据访问）
 * </p>
 * <p>
 * 口径移植自 ai-hedge-fund compute_technical_features(profile="short")：
 * EMA 采用 ewm(adjust=False)，MACD(12,26,9)，RSI 采用简单滚动均值法，
 * 量比=5日均量/20日均量。附加硬门所需扩展项（收盘位置/60日均额/波动率/低点比较等）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Component
public class ScreenFeatureCalculator {

	/**
	 * 年化交易日数
	 */
	private static final int ANNUAL_TRADING_DAYS = 244;

	/**
	 * 连板回看上限
	 */
	private static final int CONSEC_LIMIT_LOOKBACK = 5;

	/**
	 * 计算特征向量
	 * @param bars 日线（按交易日升序，最新在最后），至少 {@link ScreenConstants#MIN_BARS} 根
	 * @param limitRatio 涨停率（主板0.10/创业科创0.20）
	 * @return 特征向量，数据不足返回 null
	 */
	public ScreenFeatures calculate(List<StockDailyEntity> bars, String tsCode, double limitRatio) {
		if (CollUtil.isEmpty(bars) || bars.size() < ScreenConstants.MIN_BARS) {
			return null;
		}
		int n = bars.size();
		double[] close = new double[n];
		double[] open = new double[n];
		double[] high = new double[n];
		double[] low = new double[n];
		double[] volume = new double[n];
		double[] amount = new double[n];
		for (int i = 0; i < n; i++) {
			StockDailyEntity bar = bars.get(i);
			close[i] = val(bar.getClose());
			open[i] = val(bar.getOpen());
			high[i] = val(bar.getHigh());
			low[i] = val(bar.getLow());
			volume[i] = val(bar.getVol());
			amount[i] = val(bar.getAmount());
		}

		ScreenFeatures f = new ScreenFeatures();
		f.setTsCode(tsCode);
		f.setBarCount(n);
		f.setClose(close[n - 1]);
		f.setOpen(open[n - 1]);
		f.setHigh(high[n - 1]);
		f.setLow(low[n - 1]);
		f.setPrevClose(close[n - 2]);
		f.setPctChg(val(bars.get(n - 1).getPctChg()));
		// tushare amount 单位千元
		f.setAmountYuan(amount[n - 1] * 1000);
		f.setOneWordBoard(open[n - 1] == high[n - 1] && high[n - 1] == low[n - 1] && low[n - 1] == close[n - 1]);

		// --- 涨停相关 ---
		double limitUpPrice = f.getPrevClose() * (1 + limitRatio);
		f.setNearLimitUp(f.getPctChg() >= limitRatio * 100 - ScreenConstants.STAGE0_LIMIT_UP_MARGIN_PCT);
		f.setLimitUpDistancePct(f.getClose() > 0 ? (limitUpPrice - f.getClose()) / f.getClose() : 0);
		f.setConsecutiveLimitUp(consecutiveLimitUp(close, limitRatio));

		// --- EMA ---
		double[] ema5 = ewm(close, 5);
		double[] ema10 = ewm(close, 10);
		double[] ema20 = ewm(close, 20);
		f.setEma5(ema5[n - 1]);
		f.setEma10(ema10[n - 1]);
		f.setEma20(ema20[n - 1]);
		f.setEma20SlopeUp(n >= 6 && ema20[n - 1] > ema20[n - 6]);
		f.setEmaAlign(ema5[n - 1] > ema10[n - 1] && ema10[n - 1] > ema20[n - 1] ? 1
				: (ema5[n - 1] < ema10[n - 1] && ema10[n - 1] < ema20[n - 1] ? -1 : 0));
		f.setEmaSpread(ema20[n - 1] > 0 ? f.getClose() / ema20[n - 1] - 1 : 0);

		// --- MACD(12,26,9) ---
		double[] ema12 = ewm(close, 12);
		double[] ema26 = ewm(close, 26);
		double[] dif = new double[n];
		for (int i = 0; i < n; i++) {
			dif[i] = ema12[i] - ema26[i];
		}
		double[] dea = ewm(dif, 9);
		double histLast = (dif[n - 1] - dea[n - 1]) * 2;
		double histPrev = (dif[n - 2] - dea[n - 2]) * 2;
		f.setMacdHist(histLast);
		f.setMacdGoldCross(histLast > 0 && histPrev <= 0);
		f.setDifNorm(f.getClose() > 0 ? dif[n - 1] / f.getClose() : 0);

		// --- RSI6（简单滚动均值法，对齐 Python 实现） ---
		f.setRsi6(rsi(close, 6));

		// --- 动量 ---
		f.setMom5(n >= 6 && close[n - 6] > 0 ? close[n - 1] / close[n - 6] - 1 : 0);
		f.setMom20(n >= 21 && close[n - 21] > 0 ? close[n - 1] / close[n - 21] - 1 : 0);

		// --- 量比（5日均量/20日均量） ---
		double volShort = avg(volume, n - 5, n);
		double volLong = avg(volume, n - Math.min(20, n), n);
		f.setVolRatio(volLong > 0 ? volShort / volLong : 1.0);

		// --- 20日突破 ---
		double high20 = max(high, n - Math.min(20, n), n);
		f.setHigh20(high20);
		f.setBreakoutUp(high20 > 0 && f.getClose() >= 0.995 * high20);

		// --- MA5/MA20 乖离 ---
		double ma5 = avg(close, n - 5, n);
		double ma20 = avg(close, n - Math.min(20, n), n);
		f.setMaRatio(ma20 > 0 ? ma5 / ma20 - 1 : 0);

		// --- 20日年化波动率 ---
		f.setVolatility20(volatility(close, 20));

		// --- 收盘位置（当日振幅上半区判定） ---
		double range = f.getHigh() - f.getLow();
		f.setClosePosition(range > 0 ? (f.getClose() - f.getLow()) / range : 1.0);

		// --- 60日日均成交额（元） ---
		int avgWindow = Math.min(60, n);
		f.setAvgAmount60Yuan(avg(amount, n - avgWindow, n) * 1000);

		// --- 低点比较（H2 下跌中继判定） ---
		f.setLow5(min(low, n - 5, n));
		f.setLow20(min(low, n - Math.min(20, n), n));

		return f;
	}

	/**
	 * ewm(adjust=False)：ema[i] = x[i]*k + ema[i-1]*(1-k)
	 */
	private double[] ewm(double[] x, int span) {
		double[] out = new double[x.length];
		double k = 2.0 / (span + 1);
		out[0] = x[0];
		for (int i = 1; i < x.length; i++) {
			out[i] = x[i] * k + out[i - 1] * (1 - k);
		}
		return out;
	}

	/**
	 * RSI（rolling mean 法）：gain/loss 的 period 均值比
	 */
	private double rsi(double[] close, int period) {
		int n = close.length;
		if (n < period + 1) {
			return 50;
		}
		double gain = 0;
		double loss = 0;
		for (int i = n - period; i < n; i++) {
			double delta = close[i] - close[i - 1];
			if (delta > 0) {
				gain += delta;
			}
			else {
				loss -= delta;
			}
		}
		double avgGain = gain / period;
		double avgLoss = loss / period;
		if (avgLoss == 0) {
			return avgGain == 0 ? 50 : 100;
		}
		double rs = avgGain / avgLoss;
		return 100 - 100 / (1 + rs);
	}

	/**
	 * 连续涨停天数（从最新一根往前数，容差0.1%）
	 */
	private int consecutiveLimitUp(double[] close, double limitRatio) {
		int n = close.length;
		int count = 0;
		int lookback = Math.min(n - 1, CONSEC_LIMIT_LOOKBACK);
		for (int i = 0; i < lookback; i++) {
			int idx = n - 1 - i;
			double prev = close[idx - 1];
			if (prev <= 0) {
				break;
			}
			double limitUpPrice = prev * (1 + limitRatio);
			if (Math.abs(close[idx] - limitUpPrice) <= limitUpPrice * 0.001) {
				count++;
			}
			else {
				break;
			}
		}
		return count;
	}

	/**
	 * 20日年化波动率（日收益率标准差 * sqrt(244)，ddof=0 对齐 numpy）
	 */
	private double volatility(double[] close, int window) {
		int n = close.length;
		int retWindow = Math.min(window, n - 1);
		if (retWindow < 2) {
			return 0;
		}
		double[] rets = new double[retWindow];
		for (int i = 0; i < retWindow; i++) {
			int idx = n - retWindow + i;
			rets[i] = close[idx - 1] > 0 ? close[idx] / close[idx - 1] - 1 : 0;
		}
		double mean = 0;
		for (double r : rets) {
			mean += r;
		}
		mean /= retWindow;
		double variance = 0;
		for (double r : rets) {
			variance += (r - mean) * (r - mean);
		}
		variance /= retWindow;
		return Math.sqrt(variance) * Math.sqrt(ANNUAL_TRADING_DAYS);
	}

	private double avg(double[] x, int from, int to) {
		int n = to - from;
		if (n <= 0) {
			return 0;
		}
		double sum = 0;
		for (int i = from; i < to; i++) {
			sum += x[i];
		}
		return sum / n;
	}

	private double max(double[] x, int from, int to) {
		double m = Double.NEGATIVE_INFINITY;
		for (int i = from; i < to; i++) {
			m = Math.max(m, x[i]);
		}
		return m;
	}

	private double min(double[] x, int from, int to) {
		double m = Double.POSITIVE_INFINITY;
		for (int i = from; i < to; i++) {
			m = Math.min(m, x[i]);
		}
		return m;
	}

	private double val(Number value) {
		return value != null ? value.doubleValue() : 0d;
	}

	/**
	 * 由代码/市场推导涨停率
	 * @param market 市场类型（主板/创业板/科创板）
	 * @param name 股票名称（ST 判定）
	 */
	public static double limitRatio(String market, String name) {
		boolean st = name != null && name.toUpperCase().contains("ST");
		if (market != null && (market.contains("创业板") || market.contains("科创板"))) {
			return 0.20;
		}
		if (market != null && market.contains("北交所")) {
			return 0.30;
		}
		// 主板：ST 5%，普通 10%
		return st ? 0.05 : 0.10;
	}

	/**
	 * 反转列表（查询默认倒序）
	 */
	public static List<StockDailyEntity> reverse(List<StockDailyEntity> bars) {
		List<StockDailyEntity> copy = new java.util.ArrayList<>(bars);
		Collections.reverse(copy);
		return copy;
	}

}
