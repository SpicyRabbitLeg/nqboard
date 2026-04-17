package com.mx.nqboard.device.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.dto.IotProductDTO;
import com.mx.nqboard.device.api.dto.TransactionDTO;
import com.mx.nqboard.device.api.entity.IotCategoryEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotProductVO;
import com.mx.nqboard.device.mapper.IotCategoryMapper;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 产品管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotProductServiceImpl extends ServiceImpl<IotProductMapper, IotProductEntity> implements IotProductService {
    private final IotProductMapper productMapper;
    private final IotCategoryMapper categoryMapper;

    private final IotDynamicTableService dynamicTableService;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public IPage<IotProductVO> page(Page<IotProductEntity> page, IotProductEntity iotProduct) {
        LambdaQueryWrapper<IotProductEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.like(StrUtil.isNotBlank(iotProduct.getName()), IotProductEntity::getName, iotProduct.getName());
        queryWrapper.eq(ObjectUtil.isNotNull(iotProduct.getCategoryId()), IotProductEntity::getCategoryId, iotProduct.getCategoryId());

        IPage<IotProductEntity> productPage = productMapper.selectPage(page, queryWrapper);
        List<IotProductEntity> productRecords = productPage.getRecords();

        Map<Long, IotCategoryEntity> categoryMap = Collections.emptyMap();
        if (ObjectUtil.isNotEmpty(productRecords)) {
            List<Long> categoryIds = productRecords.stream()
                    .map(IotProductEntity::getCategoryId)
                    .filter(ObjectUtil::isNotNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(categoryIds)) {
                categoryMap = categoryMapper.selectList(Wrappers.<IotCategoryEntity>lambdaQuery()
                                .in(IotCategoryEntity::getId, categoryIds))
                        .stream()
                        .collect(Collectors.toMap(IotCategoryEntity::getId, category -> category, (a, b) -> a));
            }
        }
        final Map<Long, IotCategoryEntity> finalCategoryMap = categoryMap;

        List<IotProductVO> voRecords = productRecords.stream().map(product -> {
            IotProductVO vo = BeanUtil.copyProperties(product, IotProductVO.class);
            vo.setIotCategory(finalCategoryMap.get(product.getCategoryId()));
            return vo;
        }).collect(Collectors.toList());

        Page<IotProductVO> resultPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        resultPage.setRecords(voRecords);
        return resultPage;
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
