package com.mx.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 流程启动
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "流程启动dto")
public class FlowStartDTO {

    /**
     * 流程定义id
     */
    @NotEmpty(message = "流程定义id不能为空")
    @Schema(description = "流程定义id")
    private String procDefId;

    /**
     * 变量集合,json对象
     */
    @NotNull(message = "form表单不能为空")
    @Schema(description = "变量集合,json对象")
    private Map<String,Object> variables;
}
