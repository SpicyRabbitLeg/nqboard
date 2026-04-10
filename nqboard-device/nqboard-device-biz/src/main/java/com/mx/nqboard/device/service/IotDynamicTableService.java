package com.mx.nqboard.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.device.api.dto.IotDynamicDTO;
import com.mx.nqboard.device.api.dto.IotTableFieldDTO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 时序数据库动态表 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface IotDynamicTableService {

    /**
     * 创建时序数据库超级表
     *
     * @param tableName 超级表名称
     * @return 成功否
     */
    Boolean createStable(String tableName);


    /**
     * 创建时序数据库表
     *
     * @param tableName 表名称
     * @param superTableName 超级表名称
     * @param tags tag
     * @return 成功否
     */
    Boolean createTable(String tableName, String superTableName, String tags);


    /**
     * 创建字段
     * @param tableName 表名称
     * @param field 表字段
     * @return 成功否
     */
    Boolean createTableField(String tableName, IotTableFieldDTO field);

    /**
     * 删除字段
     *
     * @param tableName 表名称
     * @param field 字段
     * @return 成功否
     */
    Boolean deleteTableField(String tableName,String field);

    /**
     * 删除表
     * @param tableName 表名称
     * @return 成功否
     */
    Boolean deleteTable(String tableName);

    /**
     * 删除超级表
     * @param tableName 超级表名称
     * @return 成功否
     */
    Boolean deleteStable(String tableName);

    /**
     * 校验超级表是否存在
     *
     * @param tableName 表名称
     * @return 存在否
     */
    Boolean exitStable(String tableName);

    /**
     * 校验普通表是否存在
     *
     * @param tableName 表名称
     * @return 存在否
     */
    Boolean exitTable(String tableName);

    /**
     * 校验表字段是否存在
     *
     * @param tableName 表名称
     * @param filedName 字段名称
     * @return 存在否
     */
    Boolean exitTableField(String tableName,String filedName);

    /**
     * 添加单条数据
     *
     * @param tableName 表名称
     * @param fieldList 字段列表
     * @param valueList 值列表
     * @return 成功否
     */
    Boolean save(String tableName, List<String> fieldList,List<Object> valueList);

    /**
     * 根据设备id动态查询设备数据
     *
     * @param deviceId 设备id
     * @return obj
     */
    Map<String,Object> getLastByDeviceId(Long deviceId);

    /**
     * 根据设备-分页查询
     *
     * @param page page
     * @param query query
     * @return page
     */
    IPage<Map<String,Object>> getIotDynamicDevicePage(Page page, IotDynamicDTO query);
}
