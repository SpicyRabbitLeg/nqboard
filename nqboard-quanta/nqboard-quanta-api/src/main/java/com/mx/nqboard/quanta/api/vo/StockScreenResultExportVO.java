package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 每日筛选打分结果导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "每日筛选打分结果导出excel")
public class StockScreenResultExportVO {

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
     * 信号日
     */
    @Schema(description = "信号日")
    @ExcelProperty("信号日")
    private String tradeDate;

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
     * 综合打分
     */
    @Schema(description = "综合打分")
    @ExcelProperty("综合打分")
    private BigDecimal screenScore;

    /**
     * 命中模板
     */
    @Schema(description = "命中模板")
    @ExcelProperty("命中模板")
    private String pattern;

    /**
     * 是否通过
     */
    @Schema(description = "是否通过")
    @ExcelProperty("是否通过")
    private String passed;

    /**
     * 否决原因
     */
    @Schema(description = "否决原因")
    @ExcelProperty("否决原因")
    private String rejectReason;
}
