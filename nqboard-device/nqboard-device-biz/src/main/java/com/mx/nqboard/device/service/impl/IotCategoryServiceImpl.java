package com.mx.nqboard.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.device.api.entity.IotCategoryEntity;
import com.mx.nqboard.device.mapper.IotCategoryMapper;
import com.mx.nqboard.device.service.IotCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 产品分类管理 服务实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotCategoryServiceImpl extends ServiceImpl<IotCategoryMapper, IotCategoryEntity> implements IotCategoryService {

    private final IotCategoryMapper iotCategoryMapper;
}
