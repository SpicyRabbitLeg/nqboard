package com.mx.nqboard.device.consumer;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.service.IotDynamicTableService;
import com.mx.nqboard.device.service.IotProductService;
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
@RocketMQMessageListener(topic = IotDeviceConstant.DEVICE_SAVE_TOPIC, consumerGroup = DynamicTableSaveConsumer.NAME)
public class DynamicTableSaveConsumer implements RocketMQListener<String> {
    public static final String NAME = "dynamicTableSaveConsumer";

    private final IotDynamicTableService dynamicTableService;

    private final IotProductService productService;


    @Override
    public void onMessage(String message) {
        TransactionDTO transactionDto = JSON.parseObject(message, TransactionDTO.class);
        IotDeviceDTO deviceDto = JSON.parseObject(JSON.toJSONString(transactionDto.getBizObject()), IotDeviceDTO.class);

        IotProductEntity productEntity = productService.getById(deviceDto.getProductId());
        if(ObjectUtil.isNotNull(productEntity) && !dynamicTableService.exitTable(productEntity.getIdentifier())) {
            String tags = String.format("'%s','%s'", deviceDto.getIdentifier(), deviceDto.getLocation());
            dynamicTableService.createTable(deviceDto.getIdentifier(), productEntity.getIdentifier(), tags);
            log.info("设备保存成功，开始创建表： {}", message);
        }
    }
}
