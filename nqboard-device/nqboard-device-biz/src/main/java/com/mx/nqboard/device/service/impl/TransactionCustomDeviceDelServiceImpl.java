package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备删除
 *
 * @author 泥鳅压滑板
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCustomDeviceDelServiceImpl implements TransactionCustomService<IotDeviceDTO> {

    private final IotDeviceService deviceService;

    @Override
    public boolean executeLocalTransaction(IotDeviceDTO iotDeviceDto) {
        try {
            return deviceService.transactionalDelDevice(iotDeviceDto);
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，设备删除失败:{} error: {}", iotDeviceDto, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkLocalTransaction(IotDeviceDTO iotDeviceDto) {
        try {
            IotDeviceEntity deviceEntity = deviceService.getById(iotDeviceDto.getId());
            Assert.isTrue(ObjectUtil.isNull(deviceEntity),"设备删除失败");
            return true;
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，设备删除失败:{} error: {}", iotDeviceDto, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        return IotDeviceConstant.DEVICE_DEL_TOPIC;
    }
}
