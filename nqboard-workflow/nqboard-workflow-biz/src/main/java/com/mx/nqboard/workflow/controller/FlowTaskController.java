package com.mx.nqboard.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.workflow.api.dto.FlowQueryDTO;
import com.mx.nqboard.workflow.api.dto.FlowTaskDTO;
import com.mx.nqboard.workflow.service.FlowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 工作流流程任务管理 前端控制器
 * </p>
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flowTask")
@Tag(description = "flowTask", name = "工作流流程任务管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FlowTaskController {

    private final FlowTaskService flowTaskService;

    /**
     * 我发起列表
     *
     * @param page         page
     * @param flowQueryDto flowQueryDto
     * @return R
     */
    @Operation(summary = "我发起列表", description = "我发起列表")
    @GetMapping("/myProcess")
    public R myProcess(@ParameterObject Page page, @ParameterObject FlowQueryDTO flowQueryDto) {
        return R.ok(flowTaskService.myProcess(page, flowQueryDto));
    }

    /**
     * 待办列表
     *
     * @param queryDto queryDto
     * @return R
     */
    @Operation(summary = "待办列表", description = "待办列表")
    @GetMapping("/todoList")
    public R todoList(@ParameterObject Page page, @ParameterObject FlowQueryDTO queryDto) {
        return R.ok(flowTaskService.todoList(page, queryDto));
    }

    /**
     * 已办列表
     *
     * @param queryDto queryDto
     * @return R
     */
    @Operation(summary = "已办列表", description = "已办列表")
    @GetMapping("/finishedList")
    public R finishedList(@ParameterObject Page page, @ParameterObject FlowQueryDTO queryDto) {
        return R.ok(flowTaskService.finishedList(page, queryDto));
    }

    /**
     * 取消申请
     *
     * @param flowTaskDto flowTaskDto
     * @return 成功否
     */
    @Operation(summary = "取消申请", description = "取消申请")
    @SysLog("取消申请")
    @PostMapping("/stopProcess")
    @HasPermission("workflow_flowTask_stop")
    public R stopProcess(@RequestBody FlowTaskDTO flowTaskDto) {
        return R.ok(flowTaskService.stopProcess(flowTaskDto));
    }

    /**
     * 审批任务
     *
     * @param flowTaskDto flowTaskDto
     * @return 成功否
     */
    @SysLog("审批任务")
    @Operation(summary = "审批任务", description = "审批任务")
    @PostMapping("/complete")
    public R complete(@RequestBody FlowTaskDTO flowTaskDto) {
        return R.ok(flowTaskService.complete(flowTaskDto));
    }

    /**
     * 驳回任务
     *
     * @param flowTaskDto flowTaskDto
     * @return 是否成功
     */
    @SysLog("驳回任务")
    @Operation(summary = "驳回任务", description = "驳回任务")
    @PostMapping("/taskReject")
    public R taskReject(@RequestBody FlowTaskDTO flowTaskDto) {
        return R.ok(flowTaskService.taskReject(flowTaskDto));
    }


    /**
     * 流程发起时获取下一节点
     *
     * @param flowTaskDto flowTaskDto
     * @return R
     */
    @Operation(summary = "流程发起时获取下一节点", description = "根据下一节点的类型进行判断是否需要动态在页面选择人进行发送")
    @PostMapping("/getNextFlowNodeByStart")
    public R getNextFlowNodeByStart(@RequestBody FlowTaskDTO flowTaskDto) {
        return R.ok(flowTaskService.getNextFlowNodeByStart(flowTaskDto));
    }

    /**
     * 获取下一节点
     *
     * @param flowTaskDto flowTaskDto
     * @return R
     */
    @Operation(summary = "获取下一节点",description = "获取下一节点")
    @PostMapping("/getNextFlowNode")
    public R getNextFlowNode(@RequestBody FlowTaskDTO flowTaskDto) {
        return R.ok(flowTaskService.getNextFlowNode(flowTaskDto));
    }

    /**
     * 获取流程变量
     *
     * @param taskId 流程任务Id
     * @return R
     */
    @Operation(summary = "获取流程变量", description = "获取流程变量")
    @GetMapping("/processVariables/{taskId}")
    public R processVariables(@PathVariable String taskId) {
        return R.ok(flowTaskService.processVariables(taskId));
    }

    /**
     * 流程节点信息
     *
     * @param procInsId 实例id
     * @param deployId  部署id
     * @return R
     */
    @Operation(summary = "流程节点信息", description = "流程节点信息")
    @GetMapping("/flowXmlAndNode")
    public R flowXmlAndNode(@RequestParam(required = false) String procInsId, @RequestParam(required = false) String deployId) {
        return R.ok(flowTaskService.flowXmlAndNode(procInsId, deployId));
    }

    /**
     * 流程历史流转记录
     *
     * @param procInsId 实例id
     * @param deployId  部署id
     * @return list
     */
    @Operation(summary = "流程历史流转记录", description = "流程历史流转记录")
    @GetMapping("/flowRecord")
    public R flowRecord(String procInsId, String deployId) {
        return R.ok(flowTaskService.flowRecord(procInsId, deployId));
    }
}
