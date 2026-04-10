package com.mx.nqboard.device.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.dto.IotProductDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotProductVO;
import com.mx.nqboard.device.mapper.IotProductMapper;
import com.mx.nqboard.device.service.IotDynamicTableService;
import com.mx.nqboard.device.service.IotProductService;
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
import org.springframework.util.Assert;

/**
 * <p>
 * 产品管理 服务实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotProductServiceImpl extends ServiceImpl<IotProductMapper, IotProductEntity> implements IotProductService {
    private final IotProductMapper productMapper;

    private final IotDynamicTableService dynamicTableService;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public IPage<IotProductVO> page(Page page, IotProductEntity iotProduct) {
        return productMapper.selectPage(page, iotProduct);
    }

    @Override
    public Boolean saveProduct(IotProductEntity iotProduct) {
        ColumnNameValidator.validateColumnName(iotProduct.getIdentifier());
        Assert.isTrue(!dynamicTableService.exitStable(iotProduct.getIdentifier()), IotProductConstant.ERROR_PRODUCT);

        // 采用事务消息实现
        IotProductDTO iotProductDto = BeanUtil.copyProperties(iotProduct, IotProductDTO.class);
        iotProductDto.setId(IdWorker.getId());

        TransactionDTO<IotProductDTO> transactionDto = new TransactionDTO<>();
        transactionDto.setBusinessType(IotProductConstant.PRODUCT_SAVE_TOPIC);
        transactionDto.setBizObject(iotProductDto);

        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(transactionDto)).build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(IotProductConstant.PRODUCT_SAVE_TOPIC, message, null);
        Assert.isTrue(SendStatus.SEND_OK.equals(result.getSendStatus()), IotProductConstant.ERROR_PRODUCT_SAVE);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transactionalSave(IotProductDTO iotProduct) {
        return SqlHelper.retBool(productMapper.insert(iotProduct));
    }
}
