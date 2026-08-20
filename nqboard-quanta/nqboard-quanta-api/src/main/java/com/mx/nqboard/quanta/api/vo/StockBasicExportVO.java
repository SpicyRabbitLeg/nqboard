package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 股票基础信息导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股票基础信息导出excel")
public class StockBasicExportVO {

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
     * 股票代码 000001.SZ
     */
    @Schema(description = "股票代码 000001.SZ")
    @ExcelProperty("股票代码")
    private String tsCode;

    /**
     * 股票代码
     */
    @Schema(description = "股票代码")
    @ExcelProperty("symbol")
    private String symbol;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    @ExcelProperty("股票名称")
    private String name;

    /**
     * 地域
     */
    @Schema(description = "地域")
    @ExcelProperty("地域")
    private String area;

    /**
     * 行业
     */
    @Schema(description = "行业")
    @ExcelProperty("行业")
    private String industry;

    /**
     * 拼音缩写
     */
    @Schema(description = "拼音缩写")
    @ExcelProperty("拼音缩写")
    private String cnspell;

    /**
     * 市场类型：主板/创业板/科创板
     */
    @Schema(description = "市场类型")
    @ExcelProperty("市场类型")
    private String market;

    /**
     * 上市日期
     */
    @Schema(description = "上市日期")
    @ExcelProperty("上市日期")
    private String listDate;

    /**
     * 实控人名称
     */
    @Schema(description = "实控人名称")
    @ExcelProperty("实控人名称")
    private String actName;

    /**
     * 实控人企业性质
     */
    @Schema(description = "实控人企业性质")
    @ExcelProperty("实控人企业性质")
    private String actEntType;
}
