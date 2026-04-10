package com.mx.nqboard.device.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.service.IotPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设备端点
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/iotPoint")
@Tag(description = "iotPoint", name = "设备端点管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotPointController {

    private final IotPointService iotPointService;

    /**
     * 分页查询
     *
     * @param page     分页对象
     * @param iotPoint 设备端点
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotPointPage(@ParameterObject Page page, @ParameterObject IotPointEntity iotPoint) {
        LambdaQueryWrapper<IotPointEntity> wrapper = Wrappers.lambdaQuery();
        return R.ok(iotPointService.page(page, wrapper));
    }


    /**
     * 通过条件查询设备端点
     *
     * @param iotPoint 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotPointEntity iotPoint) {
        return R.ok(iotPointService.list(Wrappers.query(iotPoint)));
    }

    /**
     * 新增设备端点
     *
     * @param iotPoint 设备端点
     * @return R
     */
    @Operation(summary = "新增设备端点", description = "新增设备端点")
    @SysLog("新增设备端点")
    @PostMapping
    @HasPermission("device_iotPoint_add")
    public R save(@RequestBody IotPointEntity iotPoint) {
        return R.ok(iotPointService.save(iotPoint));
    }

    /**
     * 修改设备端点
     *
     * @param iotPoint 设备端点
     * @return R
     */
    @Operation(summary = "修改设备端点", description = "修改设备端点")
    @SysLog("修改设备端点")
    @PutMapping
    @HasPermission("device_iotPoint_edit")
    public R updateById(@RequestBody IotPointEntity iotPoint) {
        return R.ok(iotPointService.updateById(iotPoint));
    }

    /**
     * 修改或者添加设备端点
     *
     * @param iotPoint 设备端点
     * @return R
     */
    @Operation(summary = "修改或者添加设备端点", description = "修改或者添加设备端点")
    @SysLog("修改或者添加设备端点")
    @PostMapping("/saveOrUpdate")
    @HasPermission({"device_iotPoint_add", "device_iotPoint_edit"})
    public R saveOrUpdate(@Validated @RequestBody IotPointEntity iotPoint) {
        LambdaQueryWrapper<IotPointEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(IotPointEntity::getModelId, iotPoint.getModelId());
        wrapper.eq(IotPointEntity::getDeviceId, iotPoint.getDeviceId());
        if (iotPointService.count(wrapper) == 0) {
            iotPoint.setId(null);
            return R.ok(iotPointService.save(iotPoint));
        } else {
            return R.ok(iotPointService.updateById(iotPoint));
        }
    }

    /**
     * 通过id删除设备端点
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除设备端点", description = "通过id删除设备端点")
    @SysLog("通过id删除设备端点")
    @DeleteMapping
    @HasPermission("device_iotPoint_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(iotPointService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 根据设备id获取端点列表
     *
     * @param iotPoint iotPoint
     * @return list
     */
    @Operation(summary = "根据设备id获取端点列表", description = "根据设备id获取端点列表")
    @GetMapping("/getPointDetailByDeviceId")
    public R getPointDetailByDeviceId(@ParameterObject IotPointEntity iotPoint) {
        return R.ok(iotPointService.getPointDetailByDeviceId(iotPoint));
    }
}
