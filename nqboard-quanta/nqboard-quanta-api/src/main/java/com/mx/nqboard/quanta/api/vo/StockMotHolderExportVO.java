package com.mx.nqboard.quanta.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股东增减持导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股东增减持导出excel")
public class StockMotHolderExportVO {

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
     * TS代码
     */
    @Schema(description = "TS代码")
    @ExcelProperty("TS代码")
    private String tsCode;

    /**
     * 公告日期
     */
    @Schema(description = "公告日期")
    @ExcelProperty("公告日期")
    private String annDate;

    /**
     * 股东名称
     */
    @Schema(description = "股东名称")
    @ExcelProperty("股东名称")
    private String holderName;

    /**
     * 股东类型
     */
    @Schema(description = "股东类型")
    @ExcelProperty("股东类型")
    private String holderType;

    /**
     * IN增持 DE减持
     */
    @Schema(description = "IN增持 DE减持")
    @ExcelProperty("增减持类型")
    private String inDes;

    /**
     * 变动数量
     */
    @Schema(description = "变动数量")
    @ExcelProperty("变动数量")
    private BigDecimal changeVol;

    /**
     * 占流通比例(%)
     */
    @Schema(description = "占流通比例(%)")
    @ExcelProperty("占流通比例(%)")
    private BigDecimal changeRatio;

    /**
     * 变动后持股
     */
    @Schema(description = "变动后持股")
    @ExcelProperty("变动后持股")
    private BigDecimal afterShare;

    /**
     * 变动后占流通比例(%)
     */
    @Schema(description = "变动后占流通比例(%)")
    @ExcelProperty("变动后占流通比例(%)")
    private BigDecimal afterRatio;

    /**
     * 平均价格
     */
    @Schema(description = "平均价格")
    @ExcelProperty("平均价格")
    private BigDecimal avgPrice;

    /**
     * 持股总数
     */
    @Schema(description = "持股总数")
    @ExcelProperty("持股总数")
    private BigDecimal totalShare;

    /**
     * 增减持开始日期
     */
    @Schema(description = "增减持开始日期")
    @ExcelProperty("增减持开始日期")
    private String beginDate;

    /**
     * 增减持结束日期
     */
    @Schema(description = "增减持结束日期")
    @ExcelProperty("增减持结束日期")
    private String closeDate;
}
