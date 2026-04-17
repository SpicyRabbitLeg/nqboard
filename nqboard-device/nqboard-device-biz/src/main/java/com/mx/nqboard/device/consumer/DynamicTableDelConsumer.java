package com.mx.nqboard.device.consumer;

import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.service.IotDynamicTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 设备保存成功，生成时序表
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Order
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotDeviceConstant.DEVICE_DEL_TOPIC, consumerGroup = DynamicTableDelConsumer.NAME)
public class DynamicTableDelConsumer implements RocketMQListener<String> {
    public static final String NAME = "dynamicTableDelConsumer";

    private final IotDynamicTableService dynamicTableService;

    @Override
    public void onMessage(String message) {
        TransactionDTO transactionDto = JSON.parseObject(message, TransactionDTO.class);
        IotDeviceDTO deviceDto = JSON.parseObject(JSON.toJSONString(transactionDto.getBizObject()), IotDeviceDTO.class);
        if(dynamicTableService.exitTable(deviceDto.getIdentifier())) {
            dynamicTableService.deleteTable(deviceDto.getIdentifier());
            log.info("设备删除成功，开始删除表： {}", message);
        }
    }
}
