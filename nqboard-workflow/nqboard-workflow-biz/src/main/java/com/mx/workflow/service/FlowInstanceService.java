package com.mx.workflow.service;

import org.flowable.engine.history.HistoricProcessInstance;

/**
 * @author 泥鳅压滑板
 */
public interface FlowInstanceService {

    /**
     * 删除流程实例ID
     *
     * @param instanceId   流程实例ID
     */
    void delete(String instanceId);

    /**
     * 根据实例ID查询历史实例数据
     *
     * @param processInstanceId processInstanceId
     * @return instance
     */
    HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId);
}
