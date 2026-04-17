package com.mx.nqboard.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.device.api.dto.IotDynamicDTO;
import com.mx.nqboard.device.service.IotDynamicTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 动态表管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dynamic")
@Tag(description = "dynamic", name = "动态表管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotDynamicTableController {

    private final IotDynamicTableService iotDynamicTableService;

    /**
     * 根据设备-分页查询
     *
     * @param page      分页对象
     * @param query query
     * @return R
     */
    @Operation(summary = "根据设备-分页查询", description = "根据设备-分页查询")
    @GetMapping("/page")
    public R getIotDynamicDevicePage(@ParameterObject Page page, @ParameterObject IotDynamicDTO query) {
        return R.ok(iotDynamicTableService.getIotDynamicDevicePage(page,query));
    }

    /**
     * 根据设备id获取最新一条数据
     *
     * @return R
     */
    @Operation(summary = "根据设备id获取最新一条数据",description = "根据设备id获取最新一条数据")
    @GetMapping("/getLastByDeviceId/{deviceId}")
    public R getLastByDeviceId(@PathVariable Long deviceId) {
        return R.ok(iotDynamicTableService.getLastByDeviceId(deviceId));
    }
}
