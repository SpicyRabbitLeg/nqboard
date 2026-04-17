package com.mx.nqboard.device.consumer;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotModelConstant;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.api.dto.IotTableFieldDTO;
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
@RocketMQMessageListener(topic = IotModelConstant.MODEL_SAVE_TOPIC, consumerGroup = DynamicSaveFieldConsumer.NAME)
public class DynamicSaveFieldConsumer implements RocketMQListener<String> {
    public static final String NAME = "dynamicSaveFieldConsumer";


    private final IotDynamicTableService dynamicTableService;

    private final IotProductService productService;

    @Override
    public void onMessage(String message) {
        TransactionDTO transactionDto = JSON.parseObject(message, TransactionDTO.class);
        IotModelDTO modelDto = JSON.parseObject(JSON.toJSONString(transactionDto.getBizObject()), IotModelDTO.class);

        IotProductEntity productEntity = productService.getById(modelDto.getProductId());
        if(ObjectUtil.isNotNull(productEntity) && !dynamicTableService.exitTableField(productEntity.getIdentifier(),modelDto.getIdentifier())) {
            IotTableFieldDTO field = new IotTableFieldDTO();
            field.setName(modelDto.getIdentifier());
            field.setType(mapJavaToTdEngineType(modelDto));
            dynamicTableService.createTableField(productEntity.getIdentifier(), field);
            log.info("端点保存成功，开始创建字段： {}", message);
        }
    }

    /**
     * 根据model数据类型转tdengine数据库类型
     *
     * @param modelDto modelDto
     * @return type
     */
    private String mapJavaToTdEngineType(IotModelDTO modelDto) {
        String dataType = modelDto.getDataType();
        if (StrUtil.isBlank(dataType)) {
            throw new IllegalArgumentException("Java type cannot be null");
        }

        return switch (dataType) {
            case "timestamp", "localdatetime" -> "TIMESTAMP";
            case "bool" -> "BOOL";
            case "short" -> "SMALLINT";
            case "int" -> "INT";
            case "long" -> "BIGINT";
            case "float" -> "FLOAT";
            case "double", "decimal" -> "DOUBLE";
            case "string" -> "BINARY(255)";
            default -> throw new IllegalArgumentException("Unsupported Java type: " + dataType);
        };
    }
}
