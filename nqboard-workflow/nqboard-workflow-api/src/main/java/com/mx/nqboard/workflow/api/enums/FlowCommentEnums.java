package com.mx.nqboard.workflow.api.enums;

/**
 * 流程意见类型
 *
 * @date 2025/10/28
 */
public enum FlowCommentEnums {

    /**
     * 说明
     */
    NORMAL("1", "正常意见"),
    REBACK("2", "退回意见"),
    REJECT("3", "驳回意见"),
    DELEGATE("4", "委派意见"),
    ASSIGN("5", "转办意见"),
    STOP("6", "终止流程");

    /**
     * 类型
     */
    private final String type;

    /**
     * 说明
     */
    private final String remark;

    FlowCommentEnums(String type, String remark) {
        this.type = type;
        this.remark = remark;
    }

    public String getType() {
        return type;
    }

    public String getRemark() {
        return remark;
    }
}
