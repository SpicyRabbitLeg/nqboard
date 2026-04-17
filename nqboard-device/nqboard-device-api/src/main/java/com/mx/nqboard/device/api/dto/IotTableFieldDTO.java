package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 时序数据库字段
 *
 * @author SpicyRabbitLeg
 */
@Data
public class IotTableFieldDTO {

    /**
     * 字段名称
     */
    @Schema(description = "字段名称")
    private String name;

    /**
     * 字段类型
     */
    @Schema(description = "字段类型")
    private String type;

    /**
     * 字段长度
     */
    @Schema(description = "字段长度")
    private Integer length;

    /**
     * 字段便条
     */
    @Schema(description = "字段便条")
    private String note;

    /**
     * 标签
     */
    @Schema(description = "标签")
    private String tag;

    /**
     * 字段描述
     */
    @Schema(description = "字段描述")
    private String comment;
}
