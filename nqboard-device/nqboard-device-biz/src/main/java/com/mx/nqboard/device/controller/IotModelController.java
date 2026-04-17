package com.mx.nqboard.device.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.device.api.dto.IotImportModelDTO;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.service.IotModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 物模型管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/model")
@Tag(description = "model", name = "物模型管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotModelController {
    private final IotModelService iotModelService;

    /**
     * 分页查询
     *
     * @param page   page 分页对象
     * @param entity 物模型
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotModelPage(@ParameterObject Page page, @ParameterObject IotModelEntity entity) {
        LambdaQueryWrapper<IotModelEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StrUtil.isNotBlank(entity.getName()), IotModelEntity::getName, entity.getName());
        queryWrapper.eq(StrUtil.isNotBlank(entity.getType()), IotModelEntity::getType, entity.getType());
        queryWrapper.eq(StrUtil.isNotBlank(entity.getDataType()), IotModelEntity::getDataType, entity.getDataType());
        queryWrapper.eq(ObjectUtil.isNotNull(entity.getProductId()), IotModelEntity::getProductId, entity.getProductId());
        return R.ok(iotModelService.page(page, queryWrapper));
    }

    /**
     * 通过条件查询产品表
     *
     * @param entity 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotModelEntity entity) {
        return R.ok(iotModelService.list(Wrappers.query(entity)));
    }


    /**
     * 通过id查询
     *
     * @param id id
     * @return R  对象列表
     */
    @Operation(summary = "通过id查询", description = "通过id查询")
    @GetMapping("/detail/{id}")
    public R getDetails(@ParameterObject @PathVariable Long id) {
        return R.ok(iotModelService.getById(id));
    }


    /**
     * 通过产品id获取模型列表
     *
     * @param productId 产品id
     * @return R 对象列表
     */
    @Operation(summary = "通过产品id获取模型列表", description = "通过产品id获取模型列表")
    @GetMapping("/getModelByProduct/{productId}/{deviceId}")
    public R getModelByProduct(@PathVariable Long productId, @PathVariable Long deviceId) {
        return R.ok(iotModelService.getModelByProduct(productId, deviceId));
    }


    /**
     * 新增物模型表
     *
     * @param entity 物模型
     * @return R
     */
    @Operation(summary = "新增物模型表", description = "新增物模型表")
    @SysLog("新增物模型表")
    @PostMapping
    @HasPermission("device_model_add")
    public R save(@Validated @RequestBody IotModelEntity entity) {
        return R.ok(iotModelService.saveModel(entity));
    }

    /**
     * 修改物模型表
     *
     * @param entity 物模型
     * @return R
     */
    @Operation(summary = "修改物模型表", description = "修改物模型表")
    @SysLog("修改物模型表")
    @PutMapping
    @HasPermission("device_model_edit")
    public R updateById(@Validated @RequestBody IotModelEntity entity) {
        entity.setIdentifier(null);
        entity.setDataType(null);
        return R.ok(iotModelService.updateById(entity));
    }


    @Operation(summary = "通过id删除物模型表", description = "通过id删除物模型表")
    @SysLog("通过id删除物模型表")
    @DeleteMapping
    @HasPermission("device_model_del")
    public R deleteById(@RequestBody List<Long> ids) {
        return R.ok(iotModelService.removeModelByIds(ids));
    }

    /**
     * 通过通用物模型导入模型
     *
     * @param dto dto
     * @return R
     */
    @Operation(summary = "通过通用物模型导入模型", description = "通过通用物模型导入模型")
    @SysLog("通过通用物模型导入模型")
    @PostMapping("/importModelByTemplate")
    @HasPermission("device_model_export")
    public R importModelByTemplate(@Validated @RequestBody IotImportModelDTO dto) {
        return R.ok(iotModelService.importModelByTemplate(dto));
    }
}
