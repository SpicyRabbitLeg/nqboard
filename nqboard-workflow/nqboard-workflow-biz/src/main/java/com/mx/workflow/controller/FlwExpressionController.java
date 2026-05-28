package com.mx.workflow.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.workflow.api.entity.FlwExpressionEntity;
import com.mx.workflow.service.FlwExpressionService;
import com.pig4cloud.plugin.excel.annotation.RequestExcel;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程表达式
 *
 * @author spicy
 * @date 2025-10-23 13:50:06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flwExpression")
@Tag(description = "flwExpression", name = "流程表达式管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FlwExpressionController {

    private final FlwExpressionService flwExpressionService;

    /**
     * 分页查询
     *
     * @param page          分页对象
     * @param flwExpression 流程表达式
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getFlwExpressionPage(@ParameterObject Page page, @ParameterObject FlwExpressionEntity flwExpression) {
        LambdaQueryWrapper<FlwExpressionEntity> wrapper = Wrappers.lambdaQuery();
        return R.ok(flwExpressionService.page(page, wrapper));
    }


    /**
     * 通过条件查询流程表达式
     *
     * @param flwExpression 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject FlwExpressionEntity flwExpression) {
        return R.ok(flwExpressionService.list(Wrappers.query(flwExpression)));
    }

    /**
     * 新增流程表达式
     *
     * @param flwExpression 流程表达式
     * @return R
     */
    @Operation(summary = "新增流程表达式", description = "新增流程表达式")
    @SysLog("新增流程表达式")
    @PostMapping
    @HasPermission("workflow_flwExpression_add")
    public R save(@RequestBody FlwExpressionEntity flwExpression) {
        return R.ok(flwExpressionService.save(flwExpression));
    }

    /**
     * 修改流程表达式
     *
     * @param flwExpression 流程表达式
     * @return R
     */
    @Operation(summary = "修改流程表达式", description = "修改流程表达式")
    @SysLog("修改流程表达式")
    @PutMapping
    @HasPermission("workflow_flwExpression_edit")
    public R updateById(@RequestBody FlwExpressionEntity flwExpression) {
        return R.ok(flwExpressionService.updateById(flwExpression));
    }

    /**
     * 通过id删除流程表达式
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除流程表达式", description = "通过id删除流程表达式")
    @SysLog("通过id删除流程表达式")
    @DeleteMapping
    @HasPermission("workflow_flwExpression_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(flwExpressionService.removeBatchByIds(CollUtil.toList(ids)));
    }


    /**
     * 导出excel 表格
     *
     * @param flwExpression 查询条件
     * @param ids           导出指定ID
     * @return excel 文件流
     */
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("workflow_flwExpression_export")
    public List<FlwExpressionEntity> exportExcel(FlwExpressionEntity flwExpression, Long[] ids) {
        return flwExpressionService.list(Wrappers.lambdaQuery(flwExpression).in(ArrayUtil.isNotEmpty(ids), FlwExpressionEntity::getId, ids));
    }

    /**
     * 导入excel 表
     *
     * @param flwExpressionList 对象实体列表
     * @param bindingResult     错误信息列表
     * @return ok fail
     */
    @PostMapping("/import")
    @HasPermission("workflow_flwExpression_export")
    public R importExcel(@RequestExcel List<FlwExpressionEntity> flwExpressionList, BindingResult bindingResult) {
        return R.ok(flwExpressionService.saveBatch(flwExpressionList));
    }
}
