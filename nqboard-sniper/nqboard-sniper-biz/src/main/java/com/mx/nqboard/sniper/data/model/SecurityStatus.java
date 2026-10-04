package com.mx.nqboard.sniper.data.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 证券交易状态（照 Python security_status.py / trading_rules.calc_limit_prices 语义）：
 * </p>
 * <ul>
 * <li>suspended：源码真实行为——仅<b>非交易日</b>返回 true；交易日无 bar 属 inconclusive 保守放行
 * （不判停牌），Java 照搬不修</li>
 * <li>limitUp/limitDown：base×(1±ratio)，base=交易所发布前收盘价（tushare daily 的 pre_close，
 * 除权日已是调整后参考价）——<b>不四舍五入</b>（配合 is_limit_locked 的 0.01 容差比较，源码自洽）</li>
 * <li>ratio：ST 0.05 / 北交 0.30 / 创业(300)科创(688) 0.20 / 主板 0.10（price_limit_ratio 原文）；
 * limitUp/Down 为 null 表示无前收数据无法计算</li>
 * </ul>
 *
 * @param code 6位代码
 * @param asOf 数据日期
 * @param suspended 是否停牌（仅非交易日 true）
 * @param limitUp 涨停价（不复权口径）
 * @param limitDown 跌停价（不复权口径）
 * @param limitRatio 涨跌幅比例（0.10/0.20/0.30/0.05）
 * @param isSt 是否 ST（名称 ^\*?ST 判定）
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
public record SecurityStatus(String code, LocalDate asOf, boolean suspended, BigDecimal limitUp,
		BigDecimal limitDown, BigDecimal limitRatio, boolean isSt) {

	/** 板块限值比例（照 price_limit_ratio：仅认 300 前缀为创业板，301 落入主板 10%） */
	public static BigDecimal ratioOf(String code, boolean isSt) {
		if (isSt) {
			return new BigDecimal("0.05");
		}
		if (code.startsWith("4") || code.startsWith("8")) {
			return new BigDecimal("0.30");
		}
		if (code.startsWith("300") || code.startsWith("688")) {
			return new BigDecimal("0.20");
		}
		return new BigDecimal("0.10");
	}

}
