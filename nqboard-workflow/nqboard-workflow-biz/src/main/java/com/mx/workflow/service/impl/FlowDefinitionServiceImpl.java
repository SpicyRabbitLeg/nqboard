package com.mx.workflow.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.exception.FlowErrorException;
import com.mx.nqboard.common.core.exception.ImportFileException;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.common.security.service.NqBoardUser;
import com.mx.nqboard.common.security.util.SecurityUtils;
import com.mx.workflow.api.constant.FlowDefinitionConstant;
import com.mx.workflow.api.dto.FlowDefinitionDTO;
import com.mx.workflow.api.dto.FlowSaveXmlDTO;
import com.mx.workflow.api.dto.FlowStartDTO;
import com.mx.workflow.api.enums.FlowCommentEnums;
import com.mx.workflow.api.vo.FlowProcDefVO;
import com.mx.workflow.mapper.FlowDeployMapper;
import com.mx.workflow.service.FlowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 流程定义管理 服务类实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDefinitionServiceImpl implements FlowDefinitionService {

    private final FlowDeployMapper flowDeployMapper;

    private final RepositoryService repositoryService;

    private final RuntimeService runtimeService;

    private final TaskService taskService;

    private final IdentityService identityService;


    private static final String BPMN_FILE_SUFFIX = ".bpmn";

    @Override
    public IPage<FlowProcDefVO> getFlowDefinitionPage(Page page, FlowDefinitionDTO flowDefinitionDto) {
        return flowDeployMapper.selectDeployList(page, flowDefinitionDto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean importFile(FlowSaveXmlDTO flowSaveXmlDto) {
        try (InputStream in = new ByteArrayInputStream(flowSaveXmlDto.getXml().getBytes(StandardCharsets.UTF_8))) {
            // 部署流程
            Deployment deploy = repositoryService.createDeployment()
                    .addInputStream(flowSaveXmlDto.getName() + BPMN_FILE_SUFFIX, in)
                    .name(flowSaveXmlDto.getName())
                    .category(flowSaveXmlDto.getCategory())
                    .deploy();

            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deploy.getId())
                    .singleResult();

            repositoryService.setProcessDefinitionCategory(definition.getId(), flowSaveXmlDto.getCategory());
        } catch (Exception e) {
            log.error("关闭输入流出错", e);
            throw new ImportFileException(MsgUtils.getMessage(FlowDefinitionConstant.ERROR_SAVE));
        }

        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(List<String> deployIds) {
        for (String id : deployIds) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(id).singleResult();

            // 查询是否存在实例
            long count = runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(id).count();
            Assert.isTrue(count == 0,MsgUtils.getMessage(FlowDefinitionConstant.ERROR_EXIT));
            repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        }
    }

    @Override
    public void updateState(FlowDefinitionDTO flowDefinitionDto) {
        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                .deploymentId(flowDefinitionDto.getDeployId())
                .singleResult();

        if (StringConstants.Switch.ENABLE.equals(flowDefinitionDto.getState())) {
            repositoryService.activateProcessDefinitionById(procDef.getId(), true, null);
        } else {
            repositoryService.suspendProcessDefinitionById(procDef.getId(), true, null);
        }
    }

    @Override
    public String getXml(String deployId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
            try (InputStream inputStream = repositoryService.getResourceAsStream(
                    definition.getDeploymentId(), definition.getResourceName())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IoUtil.copy(inputStream, out);
                return IoUtil.toStr(out, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException(MsgUtils.getMessage(FlowDefinitionConstant.ERROR_READ_XML));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean startProcessInstanceById(FlowStartDTO flowStartDto) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(flowStartDto.getProcDefId())
                .latestVersion().singleResult();

        if (Objects.nonNull(processDefinition) && processDefinition.isSuspended()) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowDefinitionConstant.ERROR_HANG_UP));
        }
        Map<String, Object> variables = flowStartDto.getVariables();

        // 设置流程发起人Id到流程中
        NqBoardUser user = SecurityUtils.getUser();
        identityService.setAuthenticatedUserId(user.getId().toString());
        variables.put(FlowDefinitionConstant.PROCESS_INITIATOR, user.getId());

        // 流程发起时 跳过发起人节点
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(flowStartDto.getProcDefId(), variables);
        // 给第一步申请人节点设置任务执行人和意见
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        if (Objects.nonNull(task)) {
            taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), FlowCommentEnums.NORMAL.getType(), user.getName() + "发起流程申请");
            taskService.complete(task.getId(), variables);
        }
        return Boolean.TRUE;
    }
}
