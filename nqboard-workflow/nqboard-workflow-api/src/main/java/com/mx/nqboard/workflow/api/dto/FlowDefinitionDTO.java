package com.mx.nqboard.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.ibatis.annotations.Delete;

/**
 * 流程
 *
 * @author SpicyRabbitLeg
 */
@Data
public class FlowDefinitionDTO {

    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    private String name;

    /**
     * 流程部署id
     */
    @Schema(description = "流程部署id")
    @NotEmpty(message = "流程部署id不能为空",groups = Delete.class)
    private String deployId;

    /**
     * 流程状态( 1:激活,2:挂起）
     */
    @Schema(description = "流程状态( 1:激活,2:挂起）")
    @NotEmpty(message = "流程状态不能为空",groups = Delete.class)
    private String state;
}
