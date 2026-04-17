package com.mx.nqboard.device.service.impl;

import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 设备保存
 *
 * @author SpicyRabbitLeg
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCustomDeviceSaveServiceImpl implements TransactionCustomService<IotDeviceDTO> {

    private final IotDeviceService deviceService;

    @Override
    public boolean executeLocalTransaction(IotDeviceDTO iotDeviceDto) {
        try {
            return deviceService.transactionalSaveDevice(iotDeviceDto);
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，设备添加失败:{} error: {}", iotDeviceDto, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkLocalTransaction(IotDeviceDTO iotDeviceDto) {
        try {
            Optional.ofNullable(deviceService.getById(iotDeviceDto.getId())).orElseThrow();
            return true;
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，设备添加失败:{} error: {}", iotDeviceDto, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        return IotDeviceConstant.DEVICE_SAVE_TOPIC;
    }
}
