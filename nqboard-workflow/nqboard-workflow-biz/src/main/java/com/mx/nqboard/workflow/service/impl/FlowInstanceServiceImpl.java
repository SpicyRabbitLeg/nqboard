package com.mx.nqboard.workflow.service.impl;

import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.workflow.api.constant.FlowDefinitionConstant;
import com.mx.nqboard.workflow.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowInstanceServiceImpl implements FlowInstanceService {
    private final RepositoryService repositoryService;

    private final RuntimeService runtimeService;

    private final IdentityService identityService;

    private final TaskService taskService;

    private final HistoryService historyService;

    private final ManagementService managementService;

    @Override
    public void delete(String instanceId) {
        // 查询历史数据
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceById(instanceId);
        if (historicProcessInstance.getEndTime() != null) {
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            return;
        }
        // 删除流程实例
        runtimeService.deleteProcessInstance(instanceId, StringConstants.Other.EMPTY);
        // 删除历史流程实例
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    @Override
    public HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (Objects.isNull(historicProcessInstance)) {
            throw new FlowableObjectNotFoundException(MsgUtils.getMessage(FlowDefinitionConstant.ERROR_NULL) + processInstanceId);
        }
        return historicProcessInstance;
    }
}
