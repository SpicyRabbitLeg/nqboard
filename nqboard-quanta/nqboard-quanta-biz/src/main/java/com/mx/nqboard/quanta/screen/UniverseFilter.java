package com.mx.nqboard.quanta.screen;

import cn.hutool.core.util.StrUtil;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 股票池过滤器：Stage 0 粗滤 + Stage 0.5 硬性否决门（H1-H6）
 * </p>
 * <p>
 * H7（次日开盘跳空）在执行环节检查（模拟买入/回测），H8（大盘门）在打分环节检查，
 * H9（解禁）/H10（池内冷却）在候选池 Gate 环节检查（P4）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Component
public class UniverseFilter {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * Stage 0：当日最低成交额（元，yml quanta.screen.min-amount）
	 */
	@Value("${quanta.screen.min-amount:" + ScreenConstants.STAGE0_MIN_AMOUNT_YUAN + "}")
	private double minAmountYuan = ScreenConstants.STAGE0_MIN_AMOUNT_YUAN;

	/**
	 * Stage 0：最低上市天数（yml quanta.screen.min-listing-days）
	 */
	@Value("${quanta.screen.min-listing-days:" + ScreenConstants.STAGE0_MIN_LISTING_DAYS + "}")
	private int minListingDays = ScreenConstants.STAGE0_MIN_LISTING_DAYS;

	/**
	 * Stage 0：剔除板块（yml quanta.screen.exclude-boards，如：科创板,北交所）
	 */
	@Value("${quanta.screen.exclude-boards:科创板,北交所}")
	private String excludeBoards = "科创板,北交所";

	/**
	 * H4：近60日日均成交额下限（元，yml quanta.screen.min-avg-amount-60d）
	 */
	@Value("${quanta.screen.min-avg-amount-60d:" + ScreenConstants.H4_MIN_AVG_AMOUNT_60D + "}")
	private double minAvgAmount60d = ScreenConstants.H4_MIN_AVG_AMOUNT_60D;

	/**
	 * Stage 0 粗滤：返回 null 表示通过，否则返回否决原因
	 * @param basic 股票基础信息
	 * @param lastBar 信号日日线
	 * @param universeCodes 指数成分股集合（null 表示不过滤）
	 */
	public String stage0RejectReason(StockBasicEntity basic, com.mx.nqboard.quanta.api.entity.StockDailyEntity lastBar,
			Set<String> universeCodes) {
		String tsCode = basic.getTsCode();
		// 指数成分过滤
		if (universeCodes != null && !universeCodes.contains(tsCode)) {
			return "非指数成分股";
		}
		// ST 剔除
		String name = StrUtil.nullToEmpty(basic.getName());
		if (name.toUpperCase().contains("ST")) {
			return "ST";
		}
		// 板块剔除（科创板/北交所）
		String market = StrUtil.nullToEmpty(basic.getMarket());
		for (String board : excludeBoards.split(",")) {
			String b = board.trim();
			if (StrUtil.isNotBlank(b) && (market.contains(b) || ("北交所".equals(b) && tsCode.endsWith(".BJ")))) {
				return "剔除板块:" + b;
			}
		}
		// 上市天数
		if (StrUtil.isNotBlank(basic.getListDate())) {
			try {
				LocalDate listDate = LocalDate.parse(basic.getListDate().length() == 8
						? basic.getListDate().substring(0, 4) + "-" + basic.getListDate().substring(4, 6) + "-"
								+ basic.getListDate().substring(6, 8)
						: basic.getListDate());
				String tradeDate = lastBar.getTradeDate();
				LocalDate signalDate = LocalDate.parse(tradeDate.length() == 8
						? tradeDate.substring(0, 4) + "-" + tradeDate.substring(4, 6) + "-" + tradeDate.substring(6, 8)
						: tradeDate);
				if (signalDate.toEpochDay() - listDate.toEpochDay() < minListingDays) {
					return "上市不足" + minListingDays + "天";
				}
			}
			catch (Exception ignore) {
				// 上市日期解析失败不阻断
			}
		}
		// 停牌/无效价格
		if (lastBar.getClose() == null || lastBar.getClose().doubleValue() <= 0) {
			return "无效价格";
		}
		// 流动性：当日成交额
		if (lastBar.getAmount() != null && lastBar.getAmount().doubleValue() * 1000 < minAmountYuan) {
			return "成交额不足";
		}
		// 一字板剔除（买不进）
		if (lastBar.getOpen() != null && lastBar.getClose() != null && lastBar.getHigh() != null
				&& lastBar.getLow() != null) {
			double o = lastBar.getOpen().doubleValue();
			double h = lastBar.getHigh().doubleValue();
			double l = lastBar.getLow().doubleValue();
			double c = lastBar.getClose().doubleValue();
			if (o == h && h == l && l == c) {
				return "一字板";
			}
		}
		return null;
	}

	/**
	 * Stage 0.5 硬性否决门 H1-H6：返回失败的门列表（空列表=通过）
	 */
	public List<String> hardGateRejects(ScreenFeatures f) {
		List<String> rejects = new ArrayList<>();
		// H1：趋势之上（close>MA20 且 EMA20 上行）
		double ma20Approx = f.getEma20();
		if (!(f.getClose() > ma20Approx && f.isEma20SlopeUp())) {
			rejects.add("H1:趋势之下");
		}
		// H2：非下跌中继（5日低点高于20日低点2%）
		if (f.getLow20() > 0 && f.getLow5() <= f.getLow20() * ScreenConstants.H2_LOW_BUFFER) {
			rejects.add("H2:下跌中继");
		}
		// H3：波动率适中
		if (f.getVolatility20() > ScreenConstants.H3_MAX_VOLATILITY) {
			rejects.add("H3:波动过大");
		}
		// H4：流动性稳定（60日日均成交额）
		if (f.getAvgAmount60Yuan() < minAvgAmount60d) {
			rejects.add("H4:均额不足");
		}
		// H5：非巨量滞涨
		if (f.getVolRatio() > ScreenConstants.H5_VOL_RATIO && f.getPctChg() < ScreenConstants.H5_MAX_PCT_CHG) {
			rejects.add("H5:巨量滞涨");
		}
		// H6：收盘位于振幅上半区
		if (f.getClosePosition() < ScreenConstants.H6_MIN_CLOSE_POSITION) {
			rejects.add("H6:尾盘回落");
		}
		return rejects;
	}

}
