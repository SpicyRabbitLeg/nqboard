package com.mx.workflow.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.workflow.api.entity.FlwListenerEntity;
import com.mx.workflow.service.FlwListenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 流程监听器管理 前端控制器
 * </p>
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flowListener")
@Tag(description = "flowListener", name = "流程监听器管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FlwListenerController {

    private final FlwListenerService flwListenerService;

    /**
     * 分页查询
     *
     * @param page        分页对象
     * @param flwListener 流程监听器表
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotCategoryPage(@ParameterObject Page<FlwListenerEntity> page, @ParameterObject FlwListenerEntity flwListener) {
        LambdaQueryWrapper<FlwListenerEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(flwListener.getName()), FlwListenerEntity::getName, flwListener.getName());
        wrapper.eq(StrUtil.isNotBlank(flwListener.getType()), FlwListenerEntity::getType, flwListener.getType());
        wrapper.eq(StrUtil.isNotBlank(flwListener.getEventType()), FlwListenerEntity::getEventType, flwListener.getEventType());
        return R.ok(flwListenerService.page(page, wrapper));
    }


    /**
     * 通过条件查询流程监听器表
     *
     * @param flwListener 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject FlwListenerEntity flwListener) {
        return R.ok(flwListenerService.list(Wrappers.query(flwListener)));
    }


    /**
     * 新增流程监听器表
     *
     * @param flwListener 流程监听器表
     * @return R
     */
    @PostMapping
    @Operation(summary = "新增流程监听器表", description = "新增流程监听器表")
    @SysLog("新增流程监听器表")
    @HasPermission("workflow_flowListener_add")
    public R save(@Validated @RequestBody FlwListenerEntity flwListener) {
        return R.ok(flwListenerService.save(flwListener));
    }


    /**
     * 修改流程监听器表
     *
     * @param flwListener 流程监听器表
     * @return R
     */
    @PutMapping
    @Operation(summary = "修改流程监听器表", description = "修改流程监听器表")
    @SysLog("修改流程监听器表")
    @HasPermission("workflow_flowListener_edit")
    public R updateById(@Validated @RequestBody FlwListenerEntity flwListener) {
        return R.ok(flwListenerService.updateById(flwListener));
    }


    /**
     * 通过id删除流程监听器表
     *
     * @param ids id列表
     * @return R
     */
    @DeleteMapping
    @Operation(summary = "通过id删除流程监听器表", description = "通过id删除流程监听器表")
    @SysLog("通过id删除流程监听器表")
    @HasPermission("workflow_flowListener_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(flwListenerService.removeBatchByIds(CollUtil.toList(ids)));
    }
}
