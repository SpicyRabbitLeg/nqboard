package com.mx.nqboard.device.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类导出excel
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "分类导出excel")
public class IotCategoryExportVO {
    /**
     * 业务id
     */
    @Schema(description = "id")
    @ExcelProperty("业务id")
    private Long id;


    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;


    /**
     * 产品名称
     */
    @Schema(description = "产品名称")
    @ExcelProperty("产品名称")
    private String name;

    /**
     * 备注
     */
    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;
}
