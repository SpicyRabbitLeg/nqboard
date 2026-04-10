package com.mx.nqboard.device.api.dto;

import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import lombok.Data;

/**
 * @author 泥鳅压滑板
 */
@Data
public class DeviceProductInfoDTO {
    /**
     * 设备
     */
    private final IotDeviceEntity device;

    /**
     * 产品
     */
    private final IotProductEntity product;
}
