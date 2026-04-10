package com.mx.nqboard.device.service;

import java.util.List;

/**
 * <p>
 * 设备值 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface IotDeviceValueService {
    /**
     * 根据表名称获取表字段列表
     *
     * @param table 表名称
     * @return list
     */
    List<String> getTableFields(String table);
}
