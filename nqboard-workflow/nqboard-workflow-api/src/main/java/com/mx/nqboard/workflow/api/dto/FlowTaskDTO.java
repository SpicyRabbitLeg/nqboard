package com.mx.nqboard.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "工作流任务相关--请求参数")
public class FlowTaskDTO {
    /**
     * 任务Id
     */
    @Schema(description = "任务Id")
    private String taskId;

    /**
     * 用户Id
     */
    @Schema(description = "用户Id")
    private String userId;

    /**
     * 任务意见
     */
    @Schema(description = "任务意见")
    private String comment;

    /**
     * 流程实例Id
     */
    @Schema(description = "流程实例Id")
    private String instanceId;

    /**
     * 节点
     */
    @Schema(description = "节点")
    private String targetKey;

    /**
     * 流程环节定义ID
     */
    @Schema(description = "流程环节定义ID")
    private String deploymentId;

    /**
     * 流程环节定义ID
     */
    @Schema(description = "流程环节定义ID")
    private String defId;

    /**
     * 子执行流ID
     */
    @Schema(description = "子执行流ID")
    private String currentChildExecutionId;

    /**
     * 子执行流是否已执行
     */
    @Schema(description = "子执行流是否已执行")
    private Boolean flag;

    /**
     * 流程变量信息
     */
    @Schema(description = "流程变量信息")
    private Map<String, Object> variables;

    /**
     * 审批人
     */
    @Schema(description = "审批人")
    private String assignee;

    /**
     * 候选人
     */
    @Schema(description = "候选人")
    private List<String> candidateUsers;

    /**
     * 审批组
     */
    @Schema(description = "审批组")
    private List<String> candidateGroups;
}
