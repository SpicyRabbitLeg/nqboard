package com.mx.nqboard.device.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 时序数据库字段
 *
 * @author 泥鳅压滑板
 */
@Data
public class IotTableFieldVO {

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
}
