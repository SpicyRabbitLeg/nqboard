package com.mx.nqboard.device.service.impl;

import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

/**
 * 全局事务处理
 *
 * @author SpicyRabbitLeg
 **/
@Log4j2
@RequiredArgsConstructor
@RocketMQTransactionListener
public class TransactionExtensibleListener implements RocketMQLocalTransactionListener {

    private final TransactionCustomHandler transactionCustomHandler;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        TransactionDTO transactionDto = JSON.parseObject(new String((byte[]) message.getPayload()), TransactionDTO.class);
        try {
            boolean isSuccess  = transactionCustomHandler.executeLocalTransaction(transactionDto.getBusinessType(), transactionDto.getBizObject());
            return isSuccess ? RocketMQLocalTransactionState.UNKNOWN : RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e ){
            log.error("rocketmq本地事务回滚，消息发送失败:{} error: {}", message, e.getMessage());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        TransactionDTO transactionDto = JSON.parseObject(new String((byte[]) message.getPayload()), TransactionDTO.class);
        try {
            boolean isSuccess  = transactionCustomHandler.checkLocalTransaction(transactionDto.getBusinessType(), transactionDto.getBizObject());
            return isSuccess ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e ){
            log.error("rocketmq本地事务回滚，消息发送失败:{} error: {}", message, e.getMessage());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
}
