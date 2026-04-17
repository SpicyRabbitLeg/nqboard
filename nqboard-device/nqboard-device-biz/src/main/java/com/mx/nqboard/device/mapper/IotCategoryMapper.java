package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 产品分类管理 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface IotCategoryMapper extends BaseMapper<IotCategoryEntity> {
}
