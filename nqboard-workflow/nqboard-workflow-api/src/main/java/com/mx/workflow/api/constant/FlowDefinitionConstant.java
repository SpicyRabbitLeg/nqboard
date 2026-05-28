package com.mx.workflow.api.constant;

/**
 * 流程定义
 *
 * @author SpicyRabbitLeg
 */
public interface FlowDefinitionConstant {

    /**
     * 获取xml失败
     */
    String ERROR_READ_XML = "flow.definition.read.xml";

    /**
     * 流程已经启动或者已完成
     */
    String ERROR_START  = "flow.definition.start";

    /**
     * 流程保存失败
     */
    String ERROR_SAVE = "flow.definition.save";

    /**
     * 流程已被挂起,请先激活流程
     */
    String ERROR_HANG_UP = "flow.definition.hangUp";

    /**
     * 流程实例不存在
     */
    String ERROR_NULL = "flow.definition.null";

    /**
     * 当前流程还存在激活实例，请处理完毕后删除
     */
    String ERROR_EXIT = "flow.definition.exit";

    /**
     * nameapace
     */
    String NAMASPASE = "http://flowable.org/bpmn";

    /**
     * 会签节点
     */
    String PROCESS_MULTI_INSTANCE = "multiInstance";

    /**
     * 自定义属性 dataType
     */
    String PROCESS_CUSTOM_DATA_TYPE = "dataType";

    /**
     * 自定义属性 userType
     */
    String PROCESS_CUSTOM_USER_TYPE = "userType";

    /**
     * 初始化人员
     */
    String PROCESS_INITIATOR = "INITIATOR";

    /**
     * 单个审批人
     */
    String PROCESS_APPROVAL = "approval";

    /**
     * 动态数据
     */
    String DYNAMIC = "dynamic";

}
