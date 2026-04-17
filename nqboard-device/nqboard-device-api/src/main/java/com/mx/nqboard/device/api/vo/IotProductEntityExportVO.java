package com.mx.nqboard.device.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品导出excel
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "产品导出excel")
public class IotProductEntityExportVO {
    /**
     * id
     */
    @Schema(description="id")
    @ExcelProperty("业务id")
    private Long id;

    /**
     * 创建时间
     */
    @Schema(description="创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 分类id
     */
    @Schema(description="分类id")
    @ExcelProperty("分类id")
    private Long categoryId;

    /**
     * 产品名称
     */
    @Schema(description="产品名称")
    @ExcelProperty("产品名称")
    private String name;

    /**
     * 备注
     */
    @Schema(description="备注")
    @ExcelProperty("备注")
    private String remark;

    /**
     * 连接协议
     */
    @Schema(description="连接协议")
    @ExcelProperty("连接协议")
    private String protocol;

    /**
     * 连接协议配置项定义json
     */
    @Schema(description="连接协议配置项定义json")
    @ExcelProperty("连接协议配置项定义json")
    private String protocolValue;

    /**
     * 授权开关
     */
    @Schema(description="授权开关")
    @ExcelProperty("授权开关")
    private Boolean authSwitch;

    /**
     * 授权模式（0简单、1加密）
     */
    @Schema(description="授权模式（0简单、1加密）")
    @ExcelProperty("授权模式（0简单、1加密）")
    private String authMode;

    /**
     * 图标
     */
    @Schema(description="图标")
    @ExcelProperty("图标")
    private String img;
}
