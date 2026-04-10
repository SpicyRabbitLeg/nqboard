package com.mx.nqboard.device.service.impl;

import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.dto.IotProductDTO;
import com.mx.nqboard.device.service.IotProductService;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 产品保存
 *
 * @author 泥鳅压滑板
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCustomProductSaveServiceImpl implements TransactionCustomService<IotProductDTO> {

    private final IotProductService productService;

    @Override
    public boolean executeLocalTransaction(IotProductDTO iotProductDto) {
        try {
            return productService.transactionalSave(iotProductDto);
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，产品添加失败:{} error: {}", iotProductDto, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkLocalTransaction(IotProductDTO iotProductDto) {
        try {
            Optional.ofNullable(productService.getById(iotProductDto.getId())).orElseThrow();
            return true;
        } catch (Exception e) {
            log.error("rocketmq本地事务回滚，产品添加失败:{} error: {}", iotProductDto, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        return IotProductConstant.PRODUCT_SAVE_TOPIC;
    }
}
