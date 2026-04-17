package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品表
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface IotProductMapper extends BaseMapper<IotProductEntity> {
}
