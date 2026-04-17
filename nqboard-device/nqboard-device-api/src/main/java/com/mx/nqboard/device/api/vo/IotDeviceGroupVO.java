package com.mx.nqboard.device.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author SpicyRabbitLeg
 **/
@Data
public class IotDeviceGroupVO {
    /**
     * 父节点
     */
    @Schema(description = "父节点id")
    private Long parentId;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 设备名称
     */
    @Schema(description = "设备名称")
    private String name;
}
