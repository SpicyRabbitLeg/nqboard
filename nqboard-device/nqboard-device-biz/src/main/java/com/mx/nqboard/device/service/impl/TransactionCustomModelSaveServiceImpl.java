package com.mx.nqboard.device.service.impl;

import com.mx.nqboard.device.api.constant.IotModelConstant;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.service.IotModelService;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 端点保存
 *
 * @author SpicyRabbitLeg
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCustomModelSaveServiceImpl implements TransactionCustomService<IotModelDTO> {

    private final IotModelService iotModelService;

    @Override
    public boolean executeLocalTransaction(IotModelDTO iotModelDto) {
        try {
            return iotModelService.transactionalSave(iotModelDto);
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，端点添加失败:{} error: {}", iotModelDto, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkLocalTransaction(IotModelDTO iotModelDto) {
        try {
            Optional.ofNullable(iotModelService.getById(iotModelDto.getId())).orElseThrow();
            return true;
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，端点添加失败:{} error: {}", iotModelDto, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        return IotModelConstant.MODEL_SAVE_TOPIC;
    }
}
