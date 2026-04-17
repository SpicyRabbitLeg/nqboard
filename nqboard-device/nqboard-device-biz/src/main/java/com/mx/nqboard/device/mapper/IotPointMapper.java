package com.mx.nqboard.device.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 设备端点管理 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface IotPointMapper extends BaseMapper<IotPointEntity> {
}