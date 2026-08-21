package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个股主力资金流导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "个股主力资金流导出excel")
public class StockMoneyFlowExportVO {

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
     * TS股票代码
     */
    @Schema(description = "TS股票代码")
    @ExcelProperty("TS股票代码")
    private String tsCode;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    @ExcelProperty("股票名称")
    private String name;

    /**
     * 所属行业
     */
    @Schema(description = "所属行业")
    @ExcelProperty("所属行业")
    private String industryName;

    /**
     * 交易日期
     */
    @Schema(description = "交易日期")
    @ExcelProperty("交易日期")
    private String tradeDate;

    /**
     * 涨跌幅%
     */
    @Schema(description = "涨跌幅%")
    @ExcelProperty("涨跌幅%")
    private BigDecimal pctChg;

    /**
     * 主力净流入额(元)
     */
    @Schema(description = "主力净流入额(元)")
    @ExcelProperty("主力净流入额(元)")
    private BigDecimal mainNetInflow;

    /**
     * 主力净流入占比%
     */
    @Schema(description = "主力净流入占比%")
    @ExcelProperty("主力净流入占比%")
    private BigDecimal mainNetPct;

    /**
     * 超大单净流入额(元)
     */
    @Schema(description = "超大单净流入额(元)")
    @ExcelProperty("超大单净流入额(元)")
    private BigDecimal superLargeNet;

    /**
     * 大单净流入额(元)
     */
    @Schema(description = "大单净流入额(元)")
    @ExcelProperty("大单净流入额(元)")
    private BigDecimal largeNet;
}
