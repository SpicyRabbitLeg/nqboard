package com.mx.nqboard.device.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用物模型导出excel
 *
 * @author 泥鳅压滑板
 */
@Data
@Schema(description = "通用物模型导出excel")
public class IotModelTemplateExportVO {
    /**
     * id
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
     * 模型名称
     */
    @Schema(description = "模型名称")
    @ExcelProperty("模型名称")
    private String name;

    /**
     * 标识
     */
    @Schema(description = "标识")
    @ExcelProperty("标识")
    private String identifier;

    /**
     * 类型（0读写、1只读、2只写）
     */
    @Schema(description = "类型（0读写、1只读、2只写）")
    @ExcelProperty("类型")
    private String type;

    /**
     * 数据类型（integer、decimal、string、bool、array）
     */
    @Schema(description = "数据类型（integer、decimal、string、bool、array）")
    @ExcelProperty("数据类型")
    private String dataType;

    /**
     * 基础值
     */
    @Schema(description = "基础值")
    @ExcelProperty("基础值")
    private String baseValue;

    /**
     * 比例系数
     */
    @Schema(description = "比例系数")
    @ExcelProperty("比例系数")
    private Integer proportionalCoefficient;

    /**
     * 描述信息
     */
    @Schema(description = "描述信息")
    @ExcelProperty("描述信息")
    private String remark;

    /**
     * 正常值范围 {min:0,max:100}
     */
    @Schema(description = "正常值范围 {min:0,max:100}")
    @ExcelProperty("正常值范围")
    private String specs;

    /**
     * 单位
     */
    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String unit;
}
