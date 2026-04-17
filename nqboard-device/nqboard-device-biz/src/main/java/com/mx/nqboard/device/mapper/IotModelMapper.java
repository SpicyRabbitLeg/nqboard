package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 物模型管理 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface IotModelMapper extends BaseMapper<IotModelEntity> {
}
