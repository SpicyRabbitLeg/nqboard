package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告&媒体新闻导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "公告&媒体新闻导出excel")
public class StockMotAnnNewsExportVO {

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
     * 发布日期
     */
    @Schema(description = "发布日期")
    @ExcelProperty("发布日期")
    private String pubDate;

    /**
     * 精确发布时间
     */
    @Schema(description = "精确发布时间")
    @ExcelProperty("精确发布时间")
    private String pubDatetime;

    /**
     * 类型
     */
    @Schema(description = "ann交易所公告 media媒体新闻")
    @ExcelProperty("类型")
    private String newsType;

    /**
     * 来源
     */
    @Schema(description = "来源")
    @ExcelProperty("来源")
    private String src;

    /**
     * 标题
     */
    @Schema(description = "标题")
    @ExcelProperty("标题")
    private String title;

    /**
     * 摘要
     */
    @Schema(description = "摘要")
    @ExcelProperty("摘要")
    private String summary;

    /**
     * 原文链接
     */
    @Schema(description = "原文链接")
    @ExcelProperty("原文链接")
    private String url;
}
