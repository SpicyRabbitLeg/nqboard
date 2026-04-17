package com.mx.nqboard.device.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDeviceFieldDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.IotDynamicTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据值持久化
 *
 * @author SpicyRabbitLeg
 **/
@Log4j2
@Component
@Order
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotDeviceConstant.DEVICE_VALUE_TOPIC, consumerGroup = IotDeviceConstant.DEVICE_VALUE_CONSUMER)
public class DynamicValueConsumer implements RocketMQListener<String> {

    private final IotDynamicTableService dynamicTableService;

    private final IotDeviceService deviceService;

    @Override
    public void onMessage(String message) {
        List<IotDeviceFieldDTO> iotDeviceFieldDtoList = JSON.parseArray(message, IotDeviceFieldDTO.class);
        if(CollUtil.isNotEmpty(iotDeviceFieldDtoList)) {
            IotDeviceEntity deviceEntity = deviceService.getById(iotDeviceFieldDtoList.get(0).getDeviceId());

            String tableName = deviceEntity.getIdentifier();
            List<String> fieldList = iotDeviceFieldDtoList.stream().map(IotDeviceFieldDTO::getField).collect(Collectors.toList());
            List<Object> valueList = iotDeviceFieldDtoList.stream().map(IotDeviceFieldDTO::getValue).collect(Collectors.toList());

            fieldList.add("ts");
            valueList.add(LocalDateTime.now());
            dynamicTableService.save(tableName,fieldList,valueList);
        }
    }
}
