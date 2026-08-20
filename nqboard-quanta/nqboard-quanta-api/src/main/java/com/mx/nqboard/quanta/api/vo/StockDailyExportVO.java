package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 股票日线行情导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股票日线行情导出excel")
public class StockDailyExportVO {

    /**
     * 主键
     */
    @Schema(description = "主键")
    @ExcelProperty("主键")
    private Long id;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 股票代码
     */
    @Schema(description = "股票代码")
    @ExcelProperty("股票代码")
    private String tsCode;

    /**
     * 交易日期
     */
    @Schema(description = "交易日期")
    @ExcelProperty("交易日期")
    private String tradeDate;

    /**
     * 开盘价
     */
    @Schema(description = "开盘价")
    @ExcelProperty("开盘价")
    private Float open;

    /**
     * 最高价
     */
    @Schema(description = "最高价")
    @ExcelProperty("最高价")
    private Float high;

    /**
     * 最低价
     */
    @Schema(description = "最低价")
    @ExcelProperty("最低价")
    private Float low;

    /**
     * 收盘价
     */
    @Schema(description = "收盘价")
    @ExcelProperty("收盘价")
    private Float close;

    /**
     * 昨收价【除权价】
     */
    @Schema(description = "昨收价")
    @ExcelProperty("昨收价")
    private Float preClose;

    /**
     * 涨跌额
     */
    @Schema(description = "涨跌额")
    @ExcelProperty("涨跌额")
    private Float change;

    /**
     * 涨跌幅（%）
     */
    @Schema(description = "涨跌幅（%）")
    @ExcelProperty("涨跌幅（%）")
    private Float pctChg;

    /**
     * 成交量 （手）
     */
    @Schema(description = "成交量 （手）")
    @ExcelProperty("成交量（手）")
    private Float vol;

    /**
     * 成交额 （千元）
     */
    @Schema(description = "成交额 （千元）")
    @ExcelProperty("成交额（千元）")
    private Float amount;

    /**
     * 盘后成交量 （手）
     */
    @Schema(description = "盘后成交量 （手）")
    @ExcelProperty("盘后成交量（手）")
    private Float ahVol;

    /**
     * 盘后成交额 （千元）
     */
    @Schema(description = "盘后成交额 （千元）")
    @ExcelProperty("盘后成交额（千元）")
    private Float ahAmount;
}
