package com.mx.nqboard.device.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteDeviceService", value = ServiceNameConstants.DEVICE_SERVICE)
public interface RemoteDeviceService {

    /**
     * 获取全部开启的设备列表
     *
     * @return list
     */
    @NoToken
    @GetMapping("/iotDevice/getDeviceAll")
	R<List<IotDeviceEntity>> getDeviceAll();
}
