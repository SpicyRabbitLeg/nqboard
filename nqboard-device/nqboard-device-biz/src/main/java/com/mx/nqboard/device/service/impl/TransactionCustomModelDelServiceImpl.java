package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.mx.nqboard.device.api.constant.IotModelConstant;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.service.IotModelService;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 端点删除
 *
 * @author 泥鳅压滑板
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCustomModelDelServiceImpl implements TransactionCustomService<IotModelDTO> {

    private final IotModelService iotModelService;

    @Override
    public boolean executeLocalTransaction(IotModelDTO iotModelDto) {
        try {
            return iotModelService.transactionalDelete(iotModelDto);
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，端点删除失败:{} error: {}", iotModelDto, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkLocalTransaction(IotModelDTO iotModelDto) {
        try {
            IotModelEntity modelEntity = iotModelService.getById(iotModelDto.getId());
            Assert.isTrue(ObjectUtil.isNull(modelEntity),"端点删除失败");
            return true;
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，端点删除失败:{} error: {}", iotModelDto, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        return IotModelConstant.MODEL_DEL_TOPIC;
    }
}
