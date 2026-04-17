package com.mx.nqboard.device.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.common.security.util.SecurityUtils;
import com.mx.nqboard.device.api.dto.VideoAlarmRecordDTO;
import com.mx.nqboard.device.api.entity.IotAlarmRecordEntity;
import com.mx.nqboard.device.service.IotAlarmRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 告警记录管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/alarm-record")
@Tag(description = "alarm-record", name = "告警记录管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotAlarmRecordController {

    private final IotAlarmRecordService iotAlarmRecordService;


    /**
     * 分页查询
     *
     * @param page      分页对象
     * @param iotAlarmRecord 告警记录表
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotDevicePage(@ParameterObject Page page, @ParameterObject IotAlarmRecordEntity iotAlarmRecord) {
        LambdaQueryWrapper<IotAlarmRecordEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(ObjectUtil.isNotNull(iotAlarmRecord.getDeviceId()),IotAlarmRecordEntity::getDeviceId,iotAlarmRecord.getDeviceId());
        queryWrapper.eq(StrUtil.isNotBlank(iotAlarmRecord.getHandleStatus()),IotAlarmRecordEntity::getHandleStatus,iotAlarmRecord.getHandleStatus());
        queryWrapper.eq(IotAlarmRecordEntity::getUserId, SecurityUtils.getUser().getId());
        return R.ok(iotAlarmRecordService.page(page, queryWrapper));
    }


    /**
     * 通过条件查询告警记录表
     *
     * @param iotAlarmRecord 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotAlarmRecordEntity iotAlarmRecord) {
        return R.ok(iotAlarmRecordService.list(Wrappers.query(iotAlarmRecord)));
    }


    /**
     * 新增告警记录表
     *
     * @param iotAlarmRecord 告警记录表
     * @return R
     */
    @Operation(summary = "新增告警记录表", description = "新增告警记录表")
    @SysLog("新增告警记录表")
    @PostMapping
    @HasPermission("device_iotAlarmRecord_add")
    public R save(@Validated @RequestBody IotAlarmRecordEntity iotAlarmRecord) {
        return R.ok(iotAlarmRecordService.save(iotAlarmRecord));
    }


    /**
     * 修改告警记录表
     *
     * @param iotAlarmRecord 告警记录表
     * @return R
     */
    @Operation(summary = "修改告警记录表", description = "修改告警记录表")
    @SysLog("修改告警记录表")
    @PutMapping
    @HasPermission("device_iotAlarmRecord_edit")
    public R updateById(@Validated @RequestBody IotAlarmRecordEntity iotAlarmRecord) {
        return R.ok(iotAlarmRecordService.updateById(iotAlarmRecord));
    }


    /**
     * 通过id删除告警记录表
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除告警记录表", description = "通过id删除告警记录表")
    @SysLog("通过id删除告警记录表")
    @DeleteMapping
    @HasPermission("device_iotAlarmRecord_del")
    public R removeById(@RequestBody List<Long> ids) {
        return R.ok(iotAlarmRecordService.removeByIds(ids));
    }

	/**
	 * 告警回调接口
	 * @param alarmRecordDto alarmRecordDto
	 * @return r
	 */
	@Inner(false)
	@Operation(summary = "告警回调接口", description = "第三方系统回调接口，视频AI识别算法")
    @PostMapping("/callback")
    public R callback(@RequestBody VideoAlarmRecordDTO alarmRecordDto) {
    	return R.ok(iotAlarmRecordService.callback(alarmRecordDto));
	}
}
