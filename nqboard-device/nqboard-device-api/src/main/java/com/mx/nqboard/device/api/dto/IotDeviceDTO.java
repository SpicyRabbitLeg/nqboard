package com.mx.nqboard.device.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @author SpicyRabbitLeg
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotDeviceDTO extends IotDeviceEntity {
    /**
     * 在线设备ids
     */
    @Schema(description = "在线设备ids")
    @JsonIgnore
    private List<Long> onlineDeviceIds;


    /**
     * 离线线设备ids
     */
    @Schema(description = "离线线设备ids")
    @JsonIgnore
    private List<Long> offlineDeviceIds;

    /**
     * 幂等性uuid
     */
    @Schema(description = "幂等性uuid")
    private Long uuid;

    /**
     * 操作类型：add、edit、del
     */
    @Schema(description = "操作类型：add、edit、del")
    private String execType;
}
