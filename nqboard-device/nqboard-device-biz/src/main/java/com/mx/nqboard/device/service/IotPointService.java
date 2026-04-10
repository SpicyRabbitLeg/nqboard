package com.mx.nqboard.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotPointVO;

import java.util.List;

/**
 * <p>
 * 设备端点管理 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface IotPointService extends IService<IotPointEntity> {
    /**
     * 根据设备id获取端点列表
     *
     * @param iotPoint iotPoint
     * @return list
     */
    List<IotPointVO> getPointDetailByDeviceId(IotPointEntity iotPoint);
}
