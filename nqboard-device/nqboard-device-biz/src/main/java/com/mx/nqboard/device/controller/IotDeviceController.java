package com.mx.nqboard.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.constant.CacheConstants;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.common.security.util.SecurityUtils;
import com.mx.nqboard.device.api.dto.IotDeviceDTO;
import com.mx.nqboard.device.api.dto.WriteDeviceDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.utils.ChineseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备表
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/iotDevice")
@Tag(description = "iotDevice", name = "设备表管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotDeviceController {

    private final IotDeviceService iotDeviceService;

    /**
     * 分页查询
     *
     * @param page      分页对象
     * @param iotDevice 设备表
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotDevicePage(@ParameterObject Page page, @ParameterObject IotDeviceDTO iotDevice) {
        return R.ok(iotDeviceService.page(page, iotDevice));
    }


    /**
     * 通过条件查询设备表
     *
     * @param iotDevice 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotDeviceEntity iotDevice) {
        iotDevice.setUserId(SecurityUtils.getUser().getId());
        return R.ok(iotDeviceService.list(Wrappers.query(iotDevice)));
    }

    /**
     * 新增设备表
     *
     * @param iotDevice 设备表
     * @return R
     */
    @Operation(summary = "新增设备表", description = "新增设备表")
    @SysLog("新增设备表")
    @PostMapping
    @HasPermission("device_iotDevice_add")
    public R save(@Validated @RequestBody IotDeviceEntity iotDevice) {
        iotDevice.setUserId(SecurityUtils.getUser().getId());
        iotDevice.setIdentifier(ChineseUtils.getWord(iotDevice.getName()));
        return R.ok(iotDeviceService.saveDevice(iotDevice));
    }

    /**
     * 修改设备表
     *
     * @param iotDevice 设备表
     * @return R
     */
    @Operation(summary = "修改设备表", description = "修改设备表")
    @SysLog("修改设备表")
    @PutMapping
    @HasPermission("device_iotDevice_edit")
	@CacheEvict(value = CacheConstants.DEVICE_ALL_DETAILS, allEntries = true)
    public R updateById(@RequestBody IotDeviceEntity iotDevice) {
        iotDevice.setName(null);
        iotDevice.setProductId(null);
        iotDevice.setUserId(null);
        return R.ok(iotDeviceService.updateById(iotDevice));
    }

    /**
     * 通过id删除设备表
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除设备表", description = "通过id删除设备表")
    @SysLog("通过id删除设备表")
    @DeleteMapping
    @HasPermission("device_iotDevice_del")
    public R removeById(@RequestBody List<Long> ids) {
        return R.ok(iotDeviceService.deleteDeviceByIds(ids));
    }

    /**
     * 获取全部开启的设备列表【系统内部接口】
     *
     * @return list
     */
    @Inner
    @Operation(summary = "获取全部开启的设备列表",description = "获取全部开启的设备列表【系统内部接口】")
    @GetMapping("/getDeviceAll")
    @Cacheable(value = CacheConstants.DEVICE_ALL_DETAILS)
    public R getDeviceAll() {
        LambdaQueryWrapper<IotDeviceEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(IotDeviceEntity::getStatus, StringConstants.Switch.ENABLE);
        return R.ok(iotDeviceService.list(queryWrapper));
    }

    /**
     * 根据产品聚合设备列表
     *
     * @param iotDeviceDto iotDeviceDto
     * @return R
     */
    @Operation(summary = "根据产品聚合设备列表",description = "根据产品聚合设备列表")
    @GetMapping("/getDeviceByGroup")
    public R getDeviceByGroup(@ParameterObject IotDeviceDTO iotDeviceDto) {
        return R.ok(iotDeviceService.getDeviceByGroup(iotDeviceDto));
    }

    /**
     * 写入设备数据
     * 
     * @param writeDeviceDTO 写入设备数据传输对象
     * @return R
     */
    @Operation(summary = "写入设备数据", description = "根据设备ID和动态参数写入设备数据")
    @SysLog("写入设备数据")
    @PostMapping("/writeDevice")
    @HasPermission("device_iotDevice_write")
    public R writeDevice(@Validated @RequestBody WriteDeviceDTO writeDeviceDTO) {
        return R.ok(iotDeviceService.writeDevice(writeDeviceDTO));
    }

	/**
	 * 删除设备监控连接
	 *
	 * @param ids 设备id
	 * @return 成功否
	 */
	@SysLog("删除设备监控连接")
    @HasPermission("device_iotDevice_del")
    @DeleteMapping("/deleteVideoByDevice")
    public R deleteVideoByDevice(@RequestBody Long[] ids) {
    	return R.ok(iotDeviceService.deleteVideoByDevice(ids));
	}
}
