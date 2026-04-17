package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态表入参
 *
 * @author SpicyRabbitLeg
 */
@Data
public class IotDynamicDTO {

    /**
     * 设备id
     */
    @Schema(description = "设备id")
    private Long deviceId;

    /**
     * 过程时间
     */
    @Schema(description = "过程时间")
    private List<LocalDateTime> filterTime;
}
