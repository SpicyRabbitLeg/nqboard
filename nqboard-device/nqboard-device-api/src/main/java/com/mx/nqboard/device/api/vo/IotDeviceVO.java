package com.mx.nqboard.device.api.vo;

import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 * @author SpicyRabbitLeg
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotDeviceVO extends IotDeviceEntity {

    /**
     * 产品对象
     */
    @Schema(description = "产品对象")
    private IotProductEntity product;

    /**
     * 在线状态
     */
    @Schema(description = "在线状态")
    private Boolean online;
}
