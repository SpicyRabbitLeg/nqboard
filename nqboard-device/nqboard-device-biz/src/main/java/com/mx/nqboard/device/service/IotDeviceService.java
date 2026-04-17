package com.mx.nqboard.device.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.dto.WriteDeviceDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.vo.IotDeviceVO;

import java.util.List;

/**
 * <p>
 * 设备管理 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface IotDeviceService extends IService<IotDeviceEntity> {

    /**
     * 分页查询
     *
     * @param page 分页对象
     * @param entity 设备表
     * @return R
     */
    IPage<IotDeviceVO> page(Page page, IotDeviceDTO entity);

    /**
     * 根据ids批量删除设备
     *
     * @param ids ids
     * @return 成功否
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 新增设备
     *
     * @param iotDevice 设备
     * @return 成功否
     */
    Boolean saveDevice(IotDeviceEntity iotDevice);

    /**
     * 新增设备执行创表
     * @param deviceDto deviceDto
     * @return 成功否
     */
    Boolean transactionalSaveDevice(IotDeviceDTO deviceDto);

    /**
     * 删除设备
     * @param ids 设备ids
     * @return 成功否
     */
    Boolean deleteDeviceByIds(List<Long> ids);

    /**
     * 删除设备执行删表
     * @param deviceDto deviceDto
     * @return 成功否
     */
    Boolean transactionalDelDevice(IotDeviceDTO deviceDto);

    /**
     * 设置设备在线状态
     *
     * @param value    value
     * @param deviceId 设备id
     */
    void setOnlineStatus(String value, Long deviceId);

    /**
     * 根据产品聚合设备列表
     *
     * @param iotDeviceDto iotDeviceDto
     * @return list
     */
    List<Tree<String>> getDeviceByGroup(IotDeviceDTO iotDeviceDto);

    /**
     * 写入设备数据
     *
     * @param writeDeviceDTO 写入设备数据传输对象
     * @return 写入结果
     */
    Boolean writeDevice(WriteDeviceDTO writeDeviceDTO);

	/**
	 * 删除设备监控连接
	 *
	 * @param ids ids
	 * @return 成功否
	 */
	Boolean deleteVideoByDevice(Long[] ids);
}
