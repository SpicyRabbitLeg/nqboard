package com.mx.nqboard.device.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.IotDynamicDTO;
import com.mx.nqboard.device.api.dto.IotTableFieldDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.api.vo.IotTableFieldVO;
import com.mx.nqboard.device.mapper.IotDeviceMapper;
import com.mx.nqboard.device.mapper.IotDynamicTableMapper;
import com.mx.nqboard.device.mapper.IotProductMapper;
import com.mx.nqboard.device.service.IotDynamicTableService;
import com.mx.nqboard.device.service.IotPointService;
import com.mx.nqboard.device.utils.ColumnNameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotDynamicTableServiceImpl implements IotDynamicTableService {

    private final IotDynamicTableMapper dynamicTableMapper;

    private final IotDeviceMapper deviceMapper;

    private final IotPointService pointService;

    private final IotProductMapper productMapper;


    @Override
    public Boolean createStable(String tableName) {
        validator(tableName);
        dynamicTableMapper.createStableTable(tableName);
        return Boolean.TRUE;
    }

    @Override
    public Boolean createTable(String tableName, String superTableName, String tags) {
        validator(tableName);
        validator(superTableName);
        dynamicTableMapper.createTableTable(tableName, superTableName, tags);
        return Boolean.TRUE;
    }

    @Override
    public Boolean createTableField(String tableName, IotTableFieldDTO field) {
        validator(tableName);
        validator(field.getName());
        dynamicTableMapper.addStableTableFiled(tableName, field);
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteTableField(String tableName, String field) {
        validator(tableName);
        validator(field);
        dynamicTableMapper.deleteStableTableFiled(tableName, field);
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteTable(String tableName) {
        validator(tableName);
        dynamicTableMapper.deleteTable(tableName);
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteStable(String tableName) {
        validator(tableName);
        dynamicTableMapper.deleteStable(tableName);
        return Boolean.TRUE;
    }

    @Override
    public Boolean exitStable(String tableName) {
        List<String> tableList = dynamicTableMapper.selectStables(tableName);
        return SqlHelper.retBool((int) tableList.stream().filter(tableName::equals).count());
    }

    @Override
    public Boolean exitTable(String tableName) {
        List<String> tableList = dynamicTableMapper.selectTables(tableName);
        return SqlHelper.retBool((int) tableList.stream().filter(tableName::equals).count());
    }

    @Override
    public Boolean exitTableField(String tableName, String filedName) {
        List<IotTableFieldVO> tableFieldVOList = dynamicTableMapper.selectStableTableField(tableName);
        return SqlHelper.retBool((int) tableFieldVOList.stream().filter(p -> filedName.equals(p.getName())).count());
    }

    @Override
    public Boolean save(String tableName, List<String> fieldList, List<Object> valueList) {
        validator(tableName);
        dynamicTableMapper.insert(tableName, fieldList, valueList);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> getLastByDeviceId(Long deviceId) {
        IotDeviceEntity deviceEntity = deviceMapper.selectById(deviceId);
        Assert.isTrue(ObjectUtil.isNotNull(deviceEntity), MsgUtils.getMessage(IotDeviceConstant.ERROR_DEVICE_EXIT));

        IotProductEntity productEntity = productMapper.selectById(deviceEntity.getProductId());

        // 根据设备获取表字段
        IotPointEntity queryPoint = new IotPointEntity();
        queryPoint.setDeviceId(deviceId);
        List<IotPointVO> pointList = pointService.getPointDetailByDeviceId(queryPoint);
        List<String> fieldList = pointList.stream()
                .map(IotPointVO::getModel)
                .map(IotModelVO::getIdentifier)
                .collect(Collectors.toList());
        fieldList.add("ts");
        return dynamicTableMapper.selectLastByDevice(productEntity.getIdentifier(), deviceEntity.getIdentifier(), fieldList);
    }

    @Override
    public IPage<Map<String, Object>> getIotDynamicDevicePage(Page page, IotDynamicDTO query) {
        IotDeviceEntity deviceEntity = deviceMapper.selectById(query.getDeviceId());
        Assert.isTrue(ObjectUtil.isNotNull(deviceEntity), MsgUtils.getMessage(IotDeviceConstant.ERROR_DEVICE_EXIT));

        IotProductEntity productEntity = productMapper.selectById(deviceEntity.getProductId());

        // 根据设备获取表字段
        IotPointEntity queryPoint = new IotPointEntity();
        queryPoint.setDeviceId(query.getDeviceId());
        List<IotPointVO> pointList = pointService.getPointDetailByDeviceId(queryPoint);
        List<String> fieldList = pointList.stream()
                .map(IotPointVO::getModel)
                .map(IotModelVO::getIdentifier)
                .collect(Collectors.toList());
        fieldList.add("ts");
        return dynamicTableMapper.selectIotDynamicDevicePage(page, productEntity.getIdentifier(), deviceEntity.getIdentifier(),fieldList,query.getFilterTime());
    }


    /**
     * 校验入参合法性
     *
     * @param tableName tableName
     * @param field     field
     */
    private void validator(String tableName, IotTableFieldDTO field) {
        if (StrUtil.isNotBlank(tableName)) {
            ColumnNameValidator.validateColumnName(tableName);
        }

        if (ObjectUtil.isNotNull(field)) {
            ColumnNameValidator.validateColumnName(field.getName());
            ColumnNameValidator.validateColumnName(field.getType());
            if (StrUtil.isNotBlank(field.getTag())) {
                ColumnNameValidator.validateColumnName(field.getTag());
            }
            if (StrUtil.isNotBlank(field.getNote())) {
                ColumnNameValidator.validateColumnName(field.getNote());
            }
            if (StrUtil.isNotBlank(field.getComment())) {
                ColumnNameValidator.validateColumnName(field.getComment());
            }
        }
    }

    /**
     * 校验入参合法性
     *
     * @param tableName tableName
     */
    private void validator(String tableName) {
        validator(tableName, null);
    }

}
