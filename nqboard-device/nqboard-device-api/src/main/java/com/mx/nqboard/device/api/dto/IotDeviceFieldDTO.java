package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author 泥鳅压滑板
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceFieldDTO {
    /**
     * 设备id
     */
    @Schema(description = "设备id")
    private Long deviceId;

    /**
     * 字段名
     */
    @Schema(description = "字段名")
    private String field;

    /**
     * 数据值
     */
    @Schema(description = "数据值")
    private Object value;

    /**
     * 数据类型
     */
    @Schema(description = "数据类型")
    private String dataType;

    /**
     * 根据数据类型获取value
     *
     * @return type
     */
    public Object getValue() {
        String tempValue = String.valueOf(value);

        return switch (dataType) {
            case "bool" -> Boolean.valueOf(tempValue);
            case "short" -> Short.valueOf(tempValue);
            case "int" -> Integer.valueOf(tempValue);
            case "long" -> Long.valueOf(tempValue);
            case "float" -> Float.valueOf(tempValue);
            case "double", "decimal" -> Double.valueOf(tempValue);
            case "string" -> tempValue;
            default -> throw new IllegalArgumentException("Unsupported Java type: " + dataType);
        };
    }
}
