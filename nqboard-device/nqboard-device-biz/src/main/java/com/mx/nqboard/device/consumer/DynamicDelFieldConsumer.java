package com.mx.nqboard.device.consumer;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotModelConstant;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.service.IotDynamicTableService;
import com.mx.nqboard.device.service.IotProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 端点保存成功，生成时序表
 *
 * @author SpicyRabbitLeg
 **/
@Log4j2
@Component
@Order
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotModelConstant.MODEL_DEL_TOPIC, consumerGroup = DynamicDelFieldConsumer.NAME)
public class DynamicDelFieldConsumer implements RocketMQListener<String> {
    public static final String NAME = "dynamicDelFieldConsumer";

    private final IotDynamicTableService dynamicTableService;

    private final IotProductService productService;

    @Override
    public void onMessage(String message) {
        TransactionDTO transactionDto = JSON.parseObject(message, TransactionDTO.class);
        IotModelDTO modelDto = JSON.parseObject(JSON.toJSONString(transactionDto.getBizObject()), IotModelDTO.class);

        IotProductEntity productEntity = productService.getById(modelDto.getProductId());
        if (ObjectUtil.isNotNull(productEntity) && dynamicTableService.exitTableField(productEntity.getIdentifier(), modelDto.getIdentifier())) {
            dynamicTableService.deleteTableField(productEntity.getIdentifier(), modelDto.getIdentifier());
            log.info("端点删除成功，开始删除字段： {}", message);
        }
    }
}
