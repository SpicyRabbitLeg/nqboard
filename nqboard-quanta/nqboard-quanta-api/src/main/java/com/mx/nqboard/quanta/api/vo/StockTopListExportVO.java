package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 龙虎榜每日明细导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "龙虎榜每日明细导出excel")
public class StockTopListExportVO {

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
     * 交易日期
     */
    @Schema(description = "交易日期")
    @ExcelProperty("交易日期")
    private String tradeDate;

    /**
     * TS代码
     */
    @Schema(description = "TS代码")
    @ExcelProperty("TS代码")
    private String tsCode;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    @ExcelProperty("股票名称")
    private String name;

    /**
     * 收盘价
     */
    @Schema(description = "收盘价")
    @ExcelProperty("收盘价")
    private BigDecimal close;

    /**
     * 涨跌幅%
     */
    @Schema(description = "涨跌幅%")
    @ExcelProperty("涨跌幅%")
    private BigDecimal pctChange;

    /**
     * 换手率%
     */
    @Schema(description = "换手率%")
    @ExcelProperty("换手率%")
    private BigDecimal turnoverRate;

    /**
     * 总成交额(元)
     */
    @Schema(description = "总成交额(元)")
    @ExcelProperty("总成交额(元)")
    private BigDecimal amount;

    /**
     * 龙虎榜卖出额
     */
    @Schema(description = "龙虎榜卖出额")
    @ExcelProperty("龙虎榜卖出额")
    private BigDecimal lSell;

    /**
     * 龙虎榜买入额
     */
    @Schema(description = "龙虎榜买入额")
    @ExcelProperty("龙虎榜买入额")
    private BigDecimal lBuy;

    /**
     * 龙虎榜成交额
     */
    @Schema(description = "龙虎榜成交额")
    @ExcelProperty("龙虎榜成交额")
    private BigDecimal lAmount;

    /**
     * 龙虎榜净买入额
     */
    @Schema(description = "龙虎榜净买入额")
    @ExcelProperty("龙虎榜净买入额")
    private BigDecimal netAmount;

    /**
     * 龙虎榜净买额占比
     */
    @Schema(description = "龙虎榜净买额占比")
    @ExcelProperty("龙虎榜净买额占比")
    private BigDecimal netRate;

    /**
     * 龙虎榜成交额占比
     */
    @Schema(description = "龙虎榜成交额占比")
    @ExcelProperty("龙虎榜成交额占比")
    private BigDecimal amountRate;

    /**
     * 当日流通市值
     */
    @Schema(description = "当日流通市值")
    @ExcelProperty("当日流通市值")
    private BigDecimal floatValues;

    /**
     * 上榜理由
     */
    @Schema(description = "上榜理由")
    @ExcelProperty("上榜理由")
    private String reason;
}
