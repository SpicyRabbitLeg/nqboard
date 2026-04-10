package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.vo.IotDeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 设备管理 Mapper 接口
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Mapper
public interface IotDeviceMapper extends BaseMapper<IotDeviceEntity> {

    /**
     * 分页查询设备
     * @param page page
     * @param entity query
     * @return page
     */
    IPage<IotDeviceVO> selectPage(Page page, @Param("entity") IotDeviceDTO entity);
}
