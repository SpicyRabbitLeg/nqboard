package com.mx.nqboard.device.api.vo;

import com.mx.nqboard.device.api.entity.IotModelEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author SpicyRabbitLeg
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotModelVO extends IotModelEntity {

    /**
     * 设备端点
     */
    @Schema(description = "设备端点")
    private IotPointVO point;
}
