package com.mx.nqboard.device.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.device.api.entity.IotCategoryEntity;
import com.mx.nqboard.device.api.vo.IotCategoryExportVO;
import com.mx.nqboard.device.service.IotCategoryService;
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
 * <p>
 * 产品分类管理 前端控制器
 * </p>
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/category")
@Tag(description = "category", name = "产品分类管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotCategoryController {

    private final IotCategoryService iotCategoryService;


    /**
     * 分页查询
     * @param page 分页对象
     * @param iotCategory 产品分类表
     * @return 分页对象
     */
    @Operation(summary = "分页查询" , description = "分页查询" )
    @GetMapping("/page" )
    public R getIotCategoryPage(@ParameterObject Page<IotCategoryEntity> page, @ParameterObject IotCategoryEntity iotCategory) {
        LambdaQueryWrapper<IotCategoryEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(iotCategory.getName()),IotCategoryEntity::getName,iotCategory.getName());
        return R.ok(iotCategoryService.page(page, wrapper));
    }


    /**
     * 通过条件查询产品分类表
     * @param iotCategory 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询" , description = "通过条件查询对象" )
    @GetMapping("/details" )
    public R getDetails(@ParameterObject IotCategoryEntity iotCategory) {
        return R.ok(iotCategoryService.list(Wrappers.query(iotCategory)));
    }


    /**
     * 新增产品分类表
     * @param iotCategory 产品分类表
     * @return R
     */
    @Operation(summary = "新增产品分类表" , description = "新增产品分类表" )
    @SysLog("新增产品分类表" )
    @PostMapping
    @HasPermission("device_iotCategory_add")
    public R save(@Validated @RequestBody IotCategoryEntity iotCategory) {
        return R.ok(iotCategoryService.save(iotCategory));
    }


    /**
     * 修改产品分类表
     * @param iotCategory 产品分类表
     * @return R
     */
    @Operation(summary = "修改产品分类表" , description = "修改产品分类表" )
    @SysLog("修改产品分类表" )
    @PutMapping
    @HasPermission("device_iotCategory_edit")
    public R updateById(@Validated @RequestBody IotCategoryEntity iotCategory) {
        return R.ok(iotCategoryService.updateById(iotCategory));
    }


    /**
     * 通过id删除产品分类表
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除产品分类表" , description = "通过id删除产品分类表" )
    @SysLog("通过id删除产品分类表" )
    @DeleteMapping
    @HasPermission("device_iotCategory_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(iotCategoryService.removeBatchByIds(CollUtil.toList(ids)));
    }


    /**
     * 导出excel 表格
     * @param iotCategory 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格" , description = "导出excel 表格" )
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("device_iotCategory_export")
    public List<IotCategoryExportVO> exportExcel(IotCategoryEntity iotCategory, Long[] ids) {
        return BeanUtil.copyToList(iotCategoryService.list(Wrappers.lambdaQuery(iotCategory).in(ArrayUtil.isNotEmpty(ids), IotCategoryEntity::getId, ids)), IotCategoryExportVO.class);
    }
}
