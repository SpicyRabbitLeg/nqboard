package com.mx.workflow.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.admin.api.entity.SysRole;
import com.mx.nqboard.admin.api.entity.SysUser;
import com.mx.nqboard.admin.api.feign.RemoteRoleService;
import com.mx.nqboard.admin.api.feign.RemoteUserService;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.workflow.api.dto.FlowDefinitionDTO;
import com.mx.workflow.api.dto.FlowSaveXmlDTO;
import com.mx.workflow.api.dto.FlowStartDTO;
import com.mx.workflow.api.entity.FlwExpressionEntity;
import com.mx.workflow.service.FlowDefinitionService;
import com.mx.workflow.service.FlwExpressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 流程定义管理 前端控制器
 * </p>
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flowDefinition")
@Tag(description = "flowDefinition", name = "流程定义管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FlowDefinitionController {

    private final FlowDefinitionService flowDefinitionService;

    private final FlwExpressionService flwExpressionService;

    private final RemoteUserService remoteUserService;

    private final RemoteRoleService remoteRoleService;

    /**
     * 分页查询
     *
     * @param page              分页对象
     * @param flowDefinitionDto 流程对象
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getFlowDefinitionPage(@ParameterObject Page page, @ParameterObject FlowDefinitionDTO flowDefinitionDto) {
        return R.ok(flowDefinitionService.getFlowDefinitionPage(page, flowDefinitionDto));
    }


    /**
     * 添加流程、保存流程设计器内的xml文件
     *
     * @param flowSaveXmlDto 流程保存
     * @return 成功否
     */
    @PostMapping
    @SysLog("添加流程")
    @HasPermission("workflow_flowDefinition_add")
    @Operation(summary = "添加流程", description = "保存流程设计器内的xml文件")
    public R save(@Validated @RequestBody FlowSaveXmlDTO flowSaveXmlDto) {
        return R.ok(flowDefinitionService.importFile(flowSaveXmlDto));
    }


    /**
     * 通过id删除删除流程表
     *
     * @return R
     */
    @DeleteMapping
    @Operation(summary = "通过id删除删除流程表", description = "通过id删除删除流程表")
    @SysLog("通过id删除删除流程表")
    @HasPermission("workflow_flowDefinition_del")
    public R deleteById(@RequestBody List<String> deployIds) {
        flowDefinitionService.deleteById(deployIds);
        return R.ok();
    }

    /**
     * 激活或挂起流程定义
     *
     * @return R
     */
    @SysLog("激活或挂起流程定义")
    @Operation(summary = "激活或挂起流程定义", description = "激活或挂起流程定义")
    @PostMapping("/updateState")
    @HasPermission("workflow_flowDefinition_edit")
    public R updateState(@Validated(Delete.class) @RequestBody FlowDefinitionDTO flowDefinitionDto) {
        flowDefinitionService.updateState(flowDefinitionDto);
        return R.ok();
    }

    /**
     * 获取xml文件
     *
     * @param deployId 部署id
     * @return R
     */
    @Operation(summary = "获取xml文件", description = "获取xml文件")
    @GetMapping("/getXml/{deployId}")
    public R getXml(@PathVariable String deployId) {
        return R.ok(flowDefinitionService.getXml(deployId));
    }

    /**
     * 发起流程
     *
     * @param flowStartDto flowStartDto
     * @return R
     */
    @SysLog("发起流程")
    @Operation(summary = "发起流程", description = "发起流程")
    @PostMapping("/start")
    @HasPermission("workflow_flowDefinition_start")
    public R start(@Validated @RequestBody FlowStartDTO flowStartDto) {
        return R.ok(flowDefinitionService.startProcessInstanceById(flowStartDto));
    }

    /**
     * 指定流程办理人员列表
     *
     * @param user user
     * @return
     */
    @Operation(summary = "指定流程办理人员列表", description = "指定流程办理人员列表")
    @GetMapping("/userList")
    public R userList(SysUser user) {
        return R.ok(RetOps.of(remoteUserService.getUserAll()).getData().orElse(Collections.emptyList()));
    }


    @Operation(summary = "指定流程办理组列表", description = "指定流程办理组列表")
    @GetMapping("/roleList")
    public R roleList(SysRole role) {
        return R.ok(RetOps.of(remoteRoleService.getDetails(new SysRole())).getData().orElse(Collections.emptyList()));
    }

    /**
     * 指定流程达式列表
     *
     * @param flwExpressionEntity flwExpressionEntity
     * @return R
     */
    @Operation(summary = "指定流程达式列表", description = "指定流程达式列表")
    @GetMapping("/expList")
    public R expList(@ParameterObject FlwExpressionEntity flwExpressionEntity) {
        return R.ok(flwExpressionService.list(Wrappers.lambdaQuery(flwExpressionEntity)));
    }
}
