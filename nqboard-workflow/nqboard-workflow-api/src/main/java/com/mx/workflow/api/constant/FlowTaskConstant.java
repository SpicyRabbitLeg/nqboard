package com.mx.workflow.api.constant;

/**
 * 流程任务
 *
 * @author SpicyRabbitLeg
 */
public interface FlowTaskConstant {

    /**
     * 流程图回显
     */
    String ERROR_FLOW_HEIGHT = "flow.task.flow.height";

    /**
     * 任务不存在
     */
    String ERROR_TASK_NULL = "flow.task.flow.null";

    /**
     * 任务挂起
     */
    String ERROR_TASK_HANG_UP = "flow.task.flow.hangUp";

    /**
     * 任务首节点
     */
    String ERROR_TASK_FIRST = "flow.task.flow.first";

    /**
     * 无法取消或开始活动
     */
    String ERROR_TASK_ERROR= "flow.task.flow.err";

    /**
     * 流程实例为空
     */
    String ERROR_TASK_FLOW_NULL="flow.task.flow.flowNull";

    /**
     * 任务出现多对多情况，无法撤回
     */
    String ERROR_TASK_MANY = "flow.task.flow.many";

    /**
     * 暂未查找到下一任务,请检查流程设计是否正确!
     */
    String ERROR_NULL_NEXT = "flow.task.flow.null.next";

    /**
     * 任务不存在或已被审批!
     */
    String ERROR_TASK_NULL_OR_USE = "flow.task.null.use";
}
