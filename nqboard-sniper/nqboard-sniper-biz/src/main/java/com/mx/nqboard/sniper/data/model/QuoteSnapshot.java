package com.mx.nqboard.sniper.data.model;

import java.math.BigDecimal;

/**
 * <p>
 * 全市场快照/批量报价统一行（对齐 Python quotes_to_spot_df schema：
 * 代码/名称/最新价/涨跌幅/成交额/成交量/今开/最高/最低，另带昨收与换手率）。
 * 单位纪律：volume 统一手（新浪股在 client 层 ÷100）；amount 统一元（腾讯万元在 client 层 ×1e4）。
 * </p>
 *
 * @param code 6位代码
 * @param name 证券简称
 * @param price 最新价
 * @param changePct 涨跌幅%
 * @param open 今开
 * @param high 最高
 * @param low 最低
 * @param prevClose 昨收
 * @param volume 成交量（手）
 * @param amount 成交额（元）
 * @param turnoverRate 换手率%（新浪源无，null）
 * @param source 来源标识 em/sina/tencent（client 构造时打标，快照表按 source 区分混存单位）
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public record QuoteSnapshot(String code, String name, BigDecimal price, BigDecimal changePct, BigDecimal open,
		BigDecimal high, BigDecimal low, BigDecimal prevClose, BigDecimal volume, BigDecimal amount,
		BigDecimal turnoverRate, String source) {
}
