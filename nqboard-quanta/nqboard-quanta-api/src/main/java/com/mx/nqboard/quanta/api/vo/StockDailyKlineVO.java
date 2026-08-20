package com.mx.nqboard.quanta.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 股票日线K线数据（前端K线图专用，轻量字段，按交易日期正序）
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股票日线K线数据")
public class StockDailyKlineVO {

    /**
     * 交易日期
     */
    @Schema(description = "交易日期")
    private String tradeDate;

    /**
     * 开盘价
     */
    @Schema(description = "开盘价")
    private Float open;

    /**
     * 最高价
     */
    @Schema(description = "最高价")
    private Float high;

    /**
     * 最低价
     */
    @Schema(description = "最低价")
    private Float low;

    /**
     * 收盘价
     */
    @Schema(description = "收盘价")
    private Float close;

    /**
     * 昨收价
     */
    @Schema(description = "昨收价")
    private Float preClose;

    /**
     * 成交量 （手）
     */
    @Schema(description = "成交量 （手）")
    private Float vol;

    /**
     * 成交额 （千元）
     */
    @Schema(description = "成交额 （千元）")
    private Float amount;

    /**
     * 涨跌幅（%）
     */
    @Schema(description = "涨跌幅（%）")
    private Float pctChg;
}
