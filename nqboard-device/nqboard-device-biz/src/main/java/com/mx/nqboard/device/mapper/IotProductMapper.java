package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产品表
 *
 * @author 泥鳅压滑板
 */
@Mapper
public interface IotProductMapper extends BaseMapper<IotProductEntity> {

    /**
     * 分页查询
     *
     * @param page page
     * @param iotProduct iotProduct
     * @return 分页对象
     */
    IPage<IotProductVO> selectPage(Page page, @Param("iotProduct") IotProductEntity iotProduct);
}
