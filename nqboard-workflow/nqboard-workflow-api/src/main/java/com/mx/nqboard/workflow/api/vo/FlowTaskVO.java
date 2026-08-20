package com.mx.nqboard.workflow.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流任务相关-返回参数
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "工作流任务相关-返回参数")
public class FlowTaskVO implements Serializable {
    /**
     * 任务编号
     */
    @Schema(description = "任务编号")
    private String taskId;

    /**
     * 任务执行编号
     */
    @Schema(description = "任务执行编号")
    private String executionId;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称")
    private String taskName;

    /**
     * 任务Key
     */
    @Schema(description = "任务Key")
    private String taskDefKey;

    /**
     * 任务执行人Id
     */
    @Schema(description = "任务执行人Id")
    private Long assigneeId;

    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 流程发起人部门名称
     */
    @Schema(description = "流程发起人部门名称")
    private String startDeptName;

    /**
     * 任务执行人名称
     */
    @Schema(description = "任务执行人名称")
    private String assigneeName;

    /**
     * 任务执行人部门
     */
    @Schema(description = "任务执行人部门")
    private String assigneeDeptName;

    /**
     * 流程发起人Id
     */
    @Schema(description = "流程发起人Id")
    private String startUserId;

    /**
     * 流程发起人名称
     */
    @Schema(description = "流程发起人名称")
    private String startUserName;

    /**
     * 流程类型
     */
    @Schema(description = "流程类型")
    private String category;

    /**
     * 流程变量信息
     */
    @Schema(description = "流程变量信息")
    private Object variables;

    /**
     * 局部变量信息
     */
    @Schema(description = "局部变量信息")
    private Object taskLocalVars;

    /**
     * 流程部署编号
     */
    @Schema(description = "流程部署编号")
    private String deployId;

    /**
     * 流程ID
     */
    @Schema(description = "流程ID")
    private String procDefId;

    /**
     * 流程key
     */
    @Schema(description = "流程key")
    private String procDefKey;

    /**
     * 流程定义名称
     */
    @Schema(description = "流程定义名称")
    private String procDefName;

    /**
     * 流程定义内置使用版本
     */
    @Schema(description = "流程定义内置使用版本")
    private Integer procDefVersion;

    /**
     * 流程实例ID
     */
    @Schema(description = "流程实例ID")
    private String procInsId;

    /**
     * 历史流程实例ID
     */
    @Schema(description = "历史流程实例ID")
    private String hisProcInsId;

    /**
     * 任务耗时
     */
    @Schema(description = "任务耗时")
    private String duration;

    /**
     * 任务意见
     */
    @Schema(description = "任务意见")
    private FlowCommentVO comment;

    /**
     * 候选执行人
     */
    @Schema(description = "候选执行人")
    private String candidate;

    /**
     * 任务创建时间
     */
    @Schema(description = "任务创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 任务完成时间
     */
    @Schema(description = "任务完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
}
