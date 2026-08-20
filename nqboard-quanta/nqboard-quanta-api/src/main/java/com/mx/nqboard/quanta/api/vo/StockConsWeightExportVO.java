package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指数成分股及权重导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "指数成分股及权重导出excel")
public class StockConsWeightExportVO {

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
     * 指数代码
     */
    @Schema(description = "指数代码")
    @ExcelProperty("指数代码")
    private String indexCode;

    /**
     * 指数名称
     */
    @Schema(description = "指数名称")
    @ExcelProperty("指数名称")
    private String indexName;

    /**
     * 指数权重(百分比)
     */
    @Schema(description = "指数权重(百分比)")
    @ExcelProperty("指数权重(%)")
    private BigDecimal weight;

    /**
     * 调样生效收盘日期
     */
    @Schema(description = "调样生效收盘日期")
    @ExcelProperty("调样生效日期")
    private LocalDate tradeDate;
}
