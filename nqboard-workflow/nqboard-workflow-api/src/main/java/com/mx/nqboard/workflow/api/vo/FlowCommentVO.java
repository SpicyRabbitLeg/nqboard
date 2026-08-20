package com.mx.nqboard.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author SpicyRabbitLeg
 */
@Data
@Schema
@Builder
public class FlowCommentVO {

    /**
     * 意见类别 0 正常意见  1 退回意见 2 驳回意见
     */
    @Schema(description = "意见类别 0 正常意见  1 退回意见 2 驳回意见")
    private String type;

    /**
     * 意见内容
     */
    @Schema(description = "意见内容")
    private String comment;
}
