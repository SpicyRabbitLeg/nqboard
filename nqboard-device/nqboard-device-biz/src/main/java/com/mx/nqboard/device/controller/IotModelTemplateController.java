package com.mx.nqboard.device.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.device.api.entity.IotModelTemplateEntity;
import com.mx.nqboard.device.api.vo.IotModelTemplateExportVO;
import com.mx.nqboard.device.service.IotModelTemplateService;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
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
 * 通用物模型表
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/modelTemplate")
@Tag(description = "modelTemplate", name = "通用物模型表管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotModelTemplateController {
    private final IotModelTemplateService iotModelTemplateService;

    /**
     * 分页查询
     *
     * @param page             分页对象
     * @param iotModelTemplate 通用物模型表
     * @return R  对象列表
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotModelTemplatePage(@ParameterObject Page page, @ParameterObject IotModelTemplateEntity iotModelTemplate) {
        LambdaQueryWrapper<IotModelTemplateEntity> wrapper = Wrappers.lambdaQuery(iotModelTemplate);
        return R.ok(iotModelTemplateService.page(page, wrapper));
    }


    /**
     * 通过条件查询通用物模型表
     *
     * @param iotModelTemplate 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotModelTemplateEntity iotModelTemplate) {
        return R.ok(iotModelTemplateService.list(Wrappers.query(iotModelTemplate)));
    }

    /**
     * 新增通用物模型表
     *
     * @param iotModelTemplate 通用物模型表
     * @return R
     */
    @Operation(summary = "新增通用物模型表", description = "新增通用物模型表")
    @SysLog("新增通用物模型表")
    @PostMapping
    @HasPermission("device_ModelTemplate_add")
    public R save(@Validated @RequestBody IotModelTemplateEntity iotModelTemplate) {
        return R.ok(iotModelTemplateService.save(iotModelTemplate));
    }

    /**
     * 修改通用物模型表
     *
     * @param iotModelTemplate 通用物模型表
     * @return R
     */
    @Operation(summary = "修改通用物模型表", description = "修改通用物模型表")
    @SysLog("修改通用物模型表")
    @PutMapping
    @HasPermission("device_ModelTemplate_edit")
    public R updateById(@Validated @RequestBody IotModelTemplateEntity iotModelTemplate) {
        return R.ok(iotModelTemplateService.updateById(iotModelTemplate));
    }

    /**
     * 通过id删除通用物模型表
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除通用物模型表", description = "通过id删除通用物模型表")
    @SysLog("通过id删除通用物模型表")
    @DeleteMapping
    @HasPermission("device_ModelTemplate_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(iotModelTemplateService.removeBatchByIds(CollUtil.toList(ids)));
    }


    /**
     * 导出excel 表格
     *
     * @param iotModelTemplate 查询条件
     * @param ids              导出指定ID
     * @return excel 文件流
     */
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("device_ModelTemplate_export")
    public List<IotModelTemplateExportVO> exportExcel(IotModelTemplateEntity iotModelTemplate, Long[] ids) {
        return BeanUtil.copyToList(iotModelTemplateService.list(Wrappers.lambdaQuery(iotModelTemplate).in(ArrayUtil.isNotEmpty(ids), IotModelTemplateEntity::getId, ids)),IotModelTemplateExportVO.class);
    }
}
