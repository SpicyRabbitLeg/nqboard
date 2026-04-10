package com.mx.nqboard.device.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.device.api.dto.IotTableFieldDTO;
import com.mx.nqboard.device.api.vo.IotTableFieldVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 时序数据库动态表
 *
 * @author 泥鳅压滑板
 */
@DS("taso_iot")
@Mapper
public interface IotDynamicTableMapper {

    /**
     * 创建一张超级表
     *
     * @param tableName 超级表名称
     */
    void createStableTable(@Param("tableName") String tableName);

    /**
     * 创建一张子表
     *
     * @param tableName      表名称
     * @param superTableName 超级表名称
     * @param tags           tags
     */
    void createTableTable(@Param("tableName") String tableName, @Param("superTableName") String superTableName, @Param("tags") String tags);

    /**
     * 添加一个字段
     *
     * @param tableName 表名称
     * @param field     字段
     */
    void addStableTableFiled(@Param("tableName") String tableName, @Param("field") IotTableFieldDTO field);


    /**
     * 修改一个字段 【数据库只支持字符类型修改，而且只能改大不能改小】
     *
     * @param tableName 表名称
     * @param field     字段
     */
    void updateStableTableFiled(@Param("tableName") String tableName, @Param("field") IotTableFieldDTO field);


    /**
     * 删除一个字段
     *
     * @param tableName 表名称
     * @param field     字段
     */
    void deleteStableTableFiled(@Param("tableName") String tableName, @Param("field") String field);


    /**
     * 根据表名称获取表字段
     *
     * @param tableName 表名称
     * @return 字段列表
     */
    List<IotTableFieldVO> selectStableTableField(@Param("tableName") String tableName);

    /**
     * 获取库中存在的创建表
     *
     * @param tableName 表名称
     * @return 列表
     */
    List<String> selectStables(@Param("tableName") String tableName);


    /**
     * 获取库中存在的普通表
     *
     * @param tableName 表名称
     * @return 列表
     */
    List<String> selectTables(@Param("tableName") String tableName);

    /**
     * 删除子表
     *
     * @param tableName 表名称
     */
    void deleteTable(String tableName);


    /**
     * 删除超级表
     *
     * @param tableName 超级表名称
     */
    void deleteStable(String tableName);

    /**
     * 添加单条数据
     *
     * @param tableName 表名
     * @param fieldList 字段列表
     * @param valueList 值列表
     */
    void insert(@Param("tableName") String tableName, @Param("fieldList") List<String> fieldList, @Param("valueList") List<Object> valueList);

    /**
     * 获取设备最新一条记录
     *
     * @param sTableName 超级表名称
     * @param deviceName 设备名称
     * @param fieldList  字段列表
     * @return last
     */
    Map<String, Object> selectLastByDevice(@Param("sTableName") String sTableName, @Param("deviceName") String deviceName, @Param("fieldList") List<String> fieldList);

    /**
     * 根据设备-分页查询
     *
     * @param page       分页对象
     * @param sTableName 超级表名称
     * @param deviceName 设备名称
     * @param fieldList  字段列表
     * @return pageResult
     */
    IPage<Map<String, Object>> selectIotDynamicDevicePage(Page page, @Param("sTableName") String sTableName, @Param("deviceName") String deviceName, @Param("fieldList") List<String> fieldList,@Param("filterTime") List<LocalDateTime> filterTime);

}

