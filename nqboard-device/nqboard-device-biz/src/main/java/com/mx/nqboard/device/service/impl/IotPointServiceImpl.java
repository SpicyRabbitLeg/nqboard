package com.mx.nqboard.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.mapper.IotPointMapper;
import com.mx.nqboard.device.service.IotPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 设备端点管理 服务实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotPointServiceImpl extends ServiceImpl<IotPointMapper, IotPointEntity> implements IotPointService {
    private final IotPointMapper pointMapper;


    @Override
    public List<IotPointVO> getPointDetailByDeviceId(IotPointEntity iotPoint) {
        return pointMapper.getPointDetailByDeviceId(iotPoint);
    }
}
