package com.mx.nqboard.device.service.impl;

import com.mx.nqboard.device.service.IotDeviceValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 设备值 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotDeviceValueServiceImpl implements IotDeviceValueService {


    @Override
    public List<String> getTableFields(String table) {
        return null;
    }
}
