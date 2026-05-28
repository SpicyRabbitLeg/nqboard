package com.mx.workflow.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 *  流程保存
 *
 * @author SpicyRabbitLeg
 */
@Data
public class FlowSaveXmlDTO {
    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    @NotEmpty(message = "流程名称不能为空")
    private String name;

    /**
     * 流程分类
     */
    @Schema(description = "流程分类")
    private String category;

    /**
     * xml 文件
     */
    @Schema(description = "xml文件")
    @NotEmpty(message = "xml文件不能为空")
    private String xml;
}
