package com.mx.nqboard.device.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 设备端点管理 Mapper 接口
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Mapper
public interface IotPointMapper extends BaseMapper<IotPointEntity> {

    /**
     * 根据设备id获取端点信息with model
     *
     * @param iotPoint iotPoint
     * @return list
     */
    List<IotPointVO> getPointDetailByDeviceId(@Param("entity") IotPointEntity iotPoint);
}