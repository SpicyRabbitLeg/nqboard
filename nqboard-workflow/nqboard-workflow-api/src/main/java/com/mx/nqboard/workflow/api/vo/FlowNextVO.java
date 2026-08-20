package com.mx.nqboard.workflow.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author SpicyRabbitLeg
 */
@Data
public class FlowNextVO implements Serializable {

    /**
     * 审批人类型
     */
    private String type;

    /**
     * 是否需要动态指定任务审批人
     */
    private String dataType;

    /**
     * 流程变量
     */
    private String vars;
}
