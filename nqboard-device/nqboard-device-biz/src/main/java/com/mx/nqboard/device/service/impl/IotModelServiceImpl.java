package com.mx.nqboard.device.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.device.api.constant.IotModelConstant;
import com.mx.nqboard.device.api.dto.IotImportModelDTO;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.api.entity.IotModelTemplateEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.mapper.IotModelMapper;
import com.mx.nqboard.device.mapper.IotModelTemplateMapper;
import com.mx.nqboard.device.mapper.IotPointMapper;
import com.mx.nqboard.device.mapper.IotProductMapper;
import com.mx.nqboard.device.service.IotDynamicTableService;
import com.mx.nqboard.device.service.IotModelService;
import com.mx.nqboard.device.utils.ColumnNameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 物模型管理 服务实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotModelServiceImpl extends ServiceImpl<IotModelMapper, IotModelEntity> implements IotModelService {
    private final IotModelMapper modelMapper;

    private final IotModelTemplateMapper modelTemplateMapper;

    private final IotProductMapper productMapper;

    private final IotPointMapper pointMapper;

    private final IotDynamicTableService dynamicTableService;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public Boolean importModelByTemplate(IotImportModelDTO dto) {
        List<IotModelTemplateEntity> templateEntityList = modelTemplateMapper.selectBatchIds(dto.getTemplateIds());
        IotProductEntity productEntity = productMapper.selectById(dto.getProductId());
        for (IotModelTemplateEntity modelTemplateEntity : templateEntityList) {
            ColumnNameValidator.validateColumnName(modelTemplateEntity.getIdentifier());
            Assert.isTrue(!dynamicTableService.exitTableField(productEntity.getIdentifier(),modelTemplateEntity.getIdentifier()), MsgUtils.getMessage(IotModelConstant.ERROR_IDENTIFIER));
        }

        // 使用事务消息处理事务问题
        for (IotModelTemplateEntity modelTemplateEntity : templateEntityList) {
            IotModelEntity iotModelEntity = BeanUtil.copyProperties(modelTemplateEntity, IotModelEntity.class,"id");
            iotModelEntity.setProductId(dto.getProductId());
            saveModel(iotModelEntity);
        }
        return Boolean.TRUE;
    }

    @Override
    public List<IotModelVO> getModelByProduct(Long productId, Long deviceId) {
        List<IotModelEntity> modelList = modelMapper.selectList(Wrappers.<IotModelEntity>lambdaQuery()
                .eq(IotModelEntity::getProductId, productId)
                .orderByAsc(IotModelEntity::getOrderNum));
        if (ObjectUtil.isEmpty(modelList)) {
            return Collections.emptyList();
        }

        List<Long> modelIds = modelList.stream().map(IotModelEntity::getId).collect(Collectors.toList());
        Map<Long, IotPointEntity> pointMap = pointMapper.selectList(Wrappers.<IotPointEntity>lambdaQuery()
                        .eq(IotPointEntity::getDeviceId, deviceId)
                        .in(IotPointEntity::getModelId, modelIds))
                .stream()
                .collect(Collectors.toMap(IotPointEntity::getModelId, point -> point, (a, b) -> a));

        return modelList.stream().map(model -> {
            IotModelVO vo = BeanUtil.copyProperties(model, IotModelVO.class);
            IotPointEntity pointEntity = pointMap.get(model.getId());
            if (ObjectUtil.isNotNull(pointEntity)) {
                vo.setPoint(BeanUtil.copyProperties(pointEntity, IotPointVO.class));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Boolean saveModel(IotModelEntity modelEntity) {
        ColumnNameValidator.validateColumnName(modelEntity.getIdentifier());

        IotProductEntity productEntity = productMapper.selectById(modelEntity.getProductId());
        Assert.isTrue(!dynamicTableService.exitTableField(productEntity.getIdentifier(),modelEntity.getIdentifier()), MsgUtils.getMessage(IotModelConstant.ERROR_IDENTIFIER));

        // 采用事务消息实现
        TransactionDTO<IotModelDTO> transactionDto = new TransactionDTO<>();

        IotModelDTO iotModelDto = BeanUtil.copyProperties(modelEntity, IotModelDTO.class);
        iotModelDto.setId(IdWorker.getId());
        transactionDto.setBizObject(iotModelDto);
        transactionDto.setBusinessType(IotModelConstant.MODEL_SAVE_TOPIC);

        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(transactionDto)).build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(IotModelConstant.MODEL_SAVE_TOPIC, message, null);
        Assert.isTrue(SendStatus.SEND_OK.equals(result.getSendStatus()), MsgUtils.getMessage(IotModelConstant.ERROR_SAVE));
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transactionalSave(IotModelDTO modelDto) {
        return SqlHelper.retBool(modelMapper.insert(modelDto));
    }

    @Override
    public Boolean removeModelByIds(List<Long> ids) {
        for (IotModelEntity modelEntity : modelMapper.selectBatchIds(ids)) {
            IotProductEntity productEntity = productMapper.selectById(modelEntity.getProductId());
            Assert.isTrue(dynamicTableService.exitTableField(productEntity.getIdentifier(),modelEntity.getIdentifier()), MsgUtils.getMessage(IotModelConstant.ERROR_IDENTIFIER_EXIT));

            // 采用事务消息实现
            TransactionDTO<IotModelDTO> transactionDto = new TransactionDTO<>();
            IotModelDTO iotModelDto = BeanUtil.copyProperties(modelEntity, IotModelDTO.class);
            transactionDto.setBizObject(iotModelDto);
            transactionDto.setBusinessType(IotModelConstant.MODEL_DEL_TOPIC);

            Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(transactionDto)).build();
            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(IotModelConstant.MODEL_DEL_TOPIC, message, null);
            Assert.isTrue(SendStatus.SEND_OK.equals(result.getSendStatus()), MsgUtils.getMessage(IotModelConstant.ERROR_DEL));
        }
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transactionalDelete(IotModelDTO modelDto) {
        return SqlHelper.retBool(modelMapper.deleteById(modelDto.getId()));
    }
}
