package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 物模型管理 Mapper 接口
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Mapper
public interface IotModelMapper extends BaseMapper<IotModelEntity> {
    /**
     * 关联查询模型并返回端点信息
     *
     * @param productId 产品id
     * @param deviceId 设备id
     * @return list
     */
    List<IotModelVO> selectModelByProduct(@Param("productId") Long productId, @Param("deviceId") Long deviceId);
}
