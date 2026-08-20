package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指数日线K线导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "指数日线K线导出excel")
public class StockIndexDailyExportVO {

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
     * 指数代码
     */
    @Schema(description = "指数代码")
    @ExcelProperty("指数代码")
    private String indexCode;

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
    private BigDecimal open;

    /**
     * 最高价
     */
    @Schema(description = "最高价")
    @ExcelProperty("最高价")
    private BigDecimal high;

    /**
     * 最低价
     */
    @Schema(description = "最低价")
    @ExcelProperty("最低价")
    private BigDecimal low;

    /**
     * 收盘价
     */
    @Schema(description = "收盘价")
    @ExcelProperty("收盘价")
    private BigDecimal close;

    /**
     * 成交量
     */
    @Schema(description = "成交量")
    @ExcelProperty("成交量")
    private BigDecimal volume;

    /**
     * 成交额(元)
     */
    @Schema(description = "成交额(元)")
    @ExcelProperty("成交额(元)")
    private BigDecimal amount;
}
