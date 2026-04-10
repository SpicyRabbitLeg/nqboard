package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 设备写入数据传输对象
 *
 * @author 泥鳅压滑板
 */
@Data
@Schema(description = "设备写入数据传输对象")
public class WriteDeviceDTO {

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    @Schema(description = "设备ID", example = "device_001")
    private String deviceId;

    /**
     * 写入数据
     * key: 数据标识符 (identifier)
     * value: 写入的值
     */
    @NotNull(message = "写入数据不能为空")
    @Schema(description = "写入数据", example = "{\"temperature\": 25.5, \"humidity\": 60, \"switch\": true}")
    private Map<String, Object> data;

    /**
     * 数据类型信息
     * key: 数据标识符 (identifier) 
     * value: 数据类型 (dataType)
     */
    @Schema(description = "数据类型信息", example = "{\"temperature\": \"float\", \"humidity\": \"int\", \"switch\": \"bool\"}")
    private Map<String, String> dataTypes;

    /**
     * 操作描述
     */
    @Schema(description = "操作描述", example = "设置温度和湿度参数")
    private String description;
}