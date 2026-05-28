package com.mx.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程任务
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "工作流任务相关--请求参数")
public class FlowQueryDTO {

    /**
     * 任务名称
     */
    @Schema(description = "任务名称")
    private String name;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private String startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private String endTime;

    /**
     * 流程keys
     */
    @Schema(description = "流程keys")
    private List<String> processDefinitionKeys;
}
