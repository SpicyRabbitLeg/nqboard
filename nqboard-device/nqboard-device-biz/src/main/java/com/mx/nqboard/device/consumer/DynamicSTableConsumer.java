package com.mx.nqboard.device.consumer;

import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.dto.IotProductDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.service.IotDynamicTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 产品保存成功，生成时序超级表
 *
 * @author 泥鳅压滑板
 **/
@Slf4j
@Component
@Order
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotProductConstant.PRODUCT_SAVE_TOPIC, consumerGroup = DynamicSTableConsumer.NAME)
public class DynamicSTableConsumer implements RocketMQListener<String> {
    public static final String NAME = "dynamicSTableConsumer";

    private final IotDynamicTableService dynamicTableService;

    @Override
    public void onMessage(String message) {
        TransactionDTO transactionDto = JSON.parseObject(message, TransactionDTO.class);

        IotProductDTO productDto = JSON.parseObject(JSON.toJSONString(transactionDto.getBizObject()),IotProductDTO.class);
        if (!dynamicTableService.exitStable(productDto.getIdentifier())) {
            dynamicTableService.createStable(productDto.getIdentifier());
            log.info("产品保存成功，开始创建时序超级表： {}", message);
        }
    }
}
