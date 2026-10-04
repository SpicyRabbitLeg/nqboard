package com.mx.nqboard.sniper.data.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 日 K 线统一模型（对齐 Python Price / 东财 K线 schema）——三家兜底源与 Tushare 共用。
 * 单位纪律（§4.3.0 权威矩阵）：volume 统一手（新浪股在 client 层 ÷100）；
 * amount 各源原样（tushare=千元、em=元、tencent/sina=null），入库映射层按 source 处理。
 * </p>
 *
 * @param code  6位代码
 * @param tradeDate 交易日
 * @param open 开盘价
 * @param high 最高价
 * @param low 最低价
 * @param close 收盘价
 * @param volume 成交量（手）
 * @param amount 成交额（各源原样单位，可空）
 * @param preClose 除权调整后昨收（仅 tushare daily 有，兜底源为 null）
 * @param source 来源标识 tushare/em/tencent/sina
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public record KlineBar(String code, LocalDate tradeDate, BigDecimal open, BigDecimal high, BigDecimal low,
		BigDecimal close, BigDecimal volume, BigDecimal amount, BigDecimal preClose, String source) {
}
