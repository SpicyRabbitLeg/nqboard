package com.mx.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 *
 * @author SpicyRabbitLeg
 */
@Data
public class FlowProcDefVO {
    /**
     * 流程id
     */
    @Schema(description = "流程id")
    private String id;

    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    private String name;

    /**
     * 流程key
     */
    @Schema(description = "流程key")
    private String flowKey;

    /**
     * 流程分类
     */
    @Schema(description = "流程分类")
    private String category;

    /**
     * 版本
     */
    @Schema(description = "版本")
    private Integer version;

    /**
     * 部署id
     */
    @Schema(description = "部署id")
    private String deploymentId;

    /**
     * 流程定义状态(1:激活 , 2:中止)
     */
    @Schema(description = "流程定义状态(1:激活 , 2:中止)")
    private Integer suspensionState;

    /**
     * 部署时间
     */
    @Schema(description = "部署时间")
    private LocalDateTime deploymentTime;
}
