package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 股东户数导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股东户数导出excel")
public class StockMotHolderCountExportVO {

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
     * 公告日期
     */
    @Schema(description = "公告日期")
    @ExcelProperty("公告日期")
    private String annDate;

    /**
     * 统计截止日期
     */
    @Schema(description = "统计截止日期")
    @ExcelProperty("统计截止日期")
    private String endDate;

    /**
     * 股东户数
     */
    @Schema(description = "股东户数")
    @ExcelProperty("股东户数")
    private Integer holderNum;
}
