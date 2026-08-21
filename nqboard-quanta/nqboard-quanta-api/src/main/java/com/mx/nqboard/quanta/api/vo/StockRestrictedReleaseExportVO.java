package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限售解禁导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "限售解禁导出excel")
public class StockRestrictedReleaseExportVO {

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
     * 解禁日期
     */
    @Schema(description = "解禁日期")
    @ExcelProperty("解禁日期")
    private String floatDate;

    /**
     * 解禁数量(万股)
     */
    @Schema(description = "解禁数量(万股)")
    @ExcelProperty("解禁数量(万股)")
    private BigDecimal floatShare;

    /**
     * 解禁数量占总股本比例%
     */
    @Schema(description = "解禁数量占总股本比例%")
    @ExcelProperty("解禁数量占总股本比例%")
    private BigDecimal floatRatio;

    /**
     * 解禁股东名称
     */
    @Schema(description = "解禁股东名称")
    @ExcelProperty("解禁股东名称")
    private String holderName;
}
