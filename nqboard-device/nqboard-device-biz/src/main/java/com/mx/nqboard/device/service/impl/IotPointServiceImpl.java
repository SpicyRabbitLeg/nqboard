package com.mx.nqboard.device.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.mapper.IotModelMapper;
import com.mx.nqboard.device.mapper.IotPointMapper;
import com.mx.nqboard.device.service.IotPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 设备端点管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotPointServiceImpl extends ServiceImpl<IotPointMapper, IotPointEntity> implements IotPointService {
    private final IotPointMapper pointMapper;
    private final IotModelMapper modelMapper;


    @Override
    public List<IotPointVO> getPointDetailByDeviceId(IotPointEntity iotPoint) {
        List<IotPointEntity> pointList = pointMapper.selectList(Wrappers.<IotPointEntity>lambdaQuery()
                .eq(IotPointEntity::getDeviceId, iotPoint.getDeviceId()));
        if (ObjectUtil.isEmpty(pointList)) {
            return Collections.emptyList();
        }

        List<Long> modelIds = pointList.stream().map(IotPointEntity::getModelId).collect(Collectors.toList());
        Map<Long, IotModelEntity> modelMap = modelMapper.selectList(Wrappers.<IotModelEntity>lambdaQuery()
                        .in(IotModelEntity::getId, modelIds))
                .stream()
                .collect(Collectors.toMap(IotModelEntity::getId, model -> model, (a, b) -> a));

        return pointList.stream().map(point -> {
                    IotPointVO vo = BeanUtil.copyProperties(point, IotPointVO.class);
                    vo.setModel(BeanUtil.copyProperties(modelMap.get(point.getModelId()), com.mx.nqboard.device.api.vo.IotModelVO.class));
                    return vo;
                }).sorted((a, b) -> {
                    Integer aOrder = ObjectUtil.isNull(a.getModel()) ? Integer.MAX_VALUE : a.getModel().getOrderNum();
                    Integer bOrder = ObjectUtil.isNull(b.getModel()) ? Integer.MAX_VALUE : b.getModel().getOrderNum();
                    return ObjectUtil.compare(aOrder, bOrder);
                })
                .collect(Collectors.toList());
    }
}
