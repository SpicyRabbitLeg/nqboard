package com.mx.nqboard.workflow.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.admin.api.entity.SysDept;
import com.mx.nqboard.admin.api.entity.SysRole;
import com.mx.nqboard.admin.api.feign.RemoteRoleService;
import com.mx.nqboard.admin.api.feign.RemoteUserService;
import com.mx.nqboard.admin.api.vo.UserVO;
import com.mx.nqboard.common.core.exception.FlowErrorException;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.common.security.service.NqBoardUser;
import com.mx.nqboard.common.security.util.SecurityUtils;
import com.mx.nqboard.workflow.api.constant.FlowDefinitionConstant;
import com.mx.nqboard.workflow.api.constant.FlowTaskConstant;
import com.mx.nqboard.workflow.api.dto.FlowQueryDTO;
import com.mx.nqboard.workflow.api.dto.FlowTaskDTO;
import com.mx.nqboard.workflow.api.enums.FlowCommentEnums;
import com.mx.nqboard.workflow.api.vo.FlowCommentVO;
import com.mx.nqboard.workflow.api.vo.FlowNextVO;
import com.mx.nqboard.workflow.api.vo.FlowTaskVO;
import com.mx.nqboard.workflow.api.vo.FlowViewerVO;
import com.mx.nqboard.workflow.service.FlowTaskService;
import com.mx.nqboard.workflow.utils.FindNextNodeUtil;
import com.mx.nqboard.workflow.utils.FlowableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 工作流流程任务管理 服务类实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTaskServiceImpl implements FlowTaskService {

    private final HistoryService historyService;

    private final RepositoryService repositoryService;

    private final TaskService taskService;

    private final RuntimeService runtimeService;

    private final RemoteUserService remoteUserService;

    private final RemoteRoleService remoteRoleService;

    @Override
    public Map<String, Object> flowXmlAndNode(String procInsId, String deployId) {
        try {
            List<FlowViewerVO> flowViewerList = new LinkedList<>();

            // 获取已经完成的节点
            List<HistoricActivityInstance> listFinished = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(procInsId)
                    .finished()
                    .list();


            // 保存已经完成的流程节点编号
            listFinished.forEach(s -> {
                FlowViewerVO flowViewerDto = new FlowViewerVO();
                flowViewerDto.setKey(s.getActivityId());
                flowViewerDto.setCompleted(true);
                // 退回节点不进行展示
                if (StringUtils.isBlank(s.getDeleteReason())) {
                    flowViewerList.add(flowViewerDto);
                }
            });

            // 获取代办节点
            List<HistoricActivityInstance> listUnFinished = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(procInsId)
                    .unfinished()
                    .list();

            // 保存需要代办的节点编号
            listUnFinished.forEach(s -> {
                // 删除已退回节点
                flowViewerList.removeIf(task -> task.getKey().equals(s.getActivityId()));
                FlowViewerVO flowViewerDto = new FlowViewerVO();
                flowViewerDto.setKey(s.getActivityId());
                flowViewerDto.setCompleted(false);
                flowViewerList.add(flowViewerDto);
            });

            // xmlData 数据
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployId).singleResult();
            InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName());
            String xmlData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            Map<String, Object> result = new HashMap<>(2);
            result.put("nodeData", flowViewerList);
            result.put("xmlData", xmlData);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(MsgUtils.getMessage(FlowTaskConstant.ERROR_FLOW_HEIGHT));
        }
    }

    @Override
    public IPage<FlowTaskVO> myProcess(Page page, FlowQueryDTO flowQueryDto) {
        NqBoardUser user = SecurityUtils.getUser();

        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .startedBy(user.getId().toString())
                .orderByProcessInstanceStartTime()
//                .processDefinitionKeyIn(flowQueryVo.getProcessDefinitionKeys()) // TODO 根据实例id查询列表而不是全部获取
                .desc();
        List<HistoricProcessInstance> historicProcessInstances = historicProcessInstanceQuery.listPage((int) ((page.getCurrent() - 1) * page.getSize()), (int) page.getSize());
        LinkedList<FlowTaskVO> flowList = new LinkedList<>();
        for (HistoricProcessInstance hisIns : historicProcessInstances) {
            FlowTaskVO flowTask = new FlowTaskVO();
            flowTask.setCreateTime(hisIns.getStartTime());
            flowTask.setFinishTime(hisIns.getEndTime());
            flowTask.setProcInsId(hisIns.getId());


            // 计算耗时
            if (Objects.nonNull(hisIns.getEndTime())) {
                long time = hisIns.getEndTime().getTime() - hisIns.getStartTime().getTime();
                flowTask.setDuration(getDateStr(time));
            } else {
                long time = System.currentTimeMillis() - hisIns.getStartTime().getTime();
                flowTask.setDuration(getDateStr(time));
            }

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(hisIns.getProcessDefinitionId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setCategory(pd.getCategory());

            // 当前所处流程
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(hisIns.getId()).list();
            if (CollectionUtils.isNotEmpty(taskList)) {
                flowTask.setTaskId(taskList.get(0).getId());
                flowTask.setTaskName(taskList.get(0).getName());
                if (StringUtils.isNotBlank(taskList.get(0).getAssignee())) {
                    // 当前任务节点办理人信息
                    UserVO sysUser = RetOps.of(remoteUserService.getDetails(Long.parseLong(taskList.get(0).getAssignee()))).getData().orElse(new UserVO());
                    flowTask.setAssigneeId(sysUser.getUserId());
                    flowTask.setAssigneeName(sysUser.getName());
                    flowTask.setAssigneeDeptName(Optional.ofNullable(sysUser.getDept()).orElse(new SysDept()).getName());
                }
            } else {
                List<HistoricTaskInstance> historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(hisIns.getId()).orderByHistoricTaskInstanceEndTime().desc().list();
                flowTask.setTaskName(historicTaskInstance.get(0).getName());
                flowTask.setTaskId(historicTaskInstance.get(0).getId());
                if (StringUtils.isNotBlank(historicTaskInstance.get(0).getAssignee())) {
                    // 当前任务节点办理人信息
                    UserVO sysUser = RetOps.of(remoteUserService.getDetails(Long.parseLong(historicTaskInstance.get(0).getAssignee()))).getData().orElse(new UserVO());
                    flowTask.setAssigneeName(sysUser.getName());
                    flowTask.setAssigneeId(sysUser.getUserId());
                    flowTask.setAssigneeDeptName(Optional.ofNullable(sysUser.getDept()).orElse(new SysDept()).getName());
                }
            }
            flowList.add(flowTask);
        }

        Page<FlowTaskVO> result = new Page<>();
        result.setTotal(historicProcessInstanceQuery.count());
        result.setRecords(flowList);
        return result;
    }

    @Override
    public IPage<FlowTaskVO> todoList(Page page, FlowQueryDTO flowQueryDto) {
        UserVO user = RetOps.of(remoteUserService.getDetails(SecurityUtils.getUser().getId())).getData().orElseThrow();

        TaskQuery taskQuery = taskService.createTaskQuery().active()
                .includeProcessVariables()
                .taskCandidateGroupIn(user.getRoleList().stream().map(role -> role.getRoleId().toString()).collect(Collectors.toList()))
                .taskCandidateOrAssigned(user.getUserId().toString())
                .orderByTaskCreateTime().desc();

        // TODO 根据表单进行过滤
        if (StringUtils.isNotBlank(flowQueryDto.getName())) {
            taskQuery.taskVariableValueLike("name", flowQueryDto.getName());
        }

        List<Task> taskList = taskQuery.listPage((int) ((page.getCurrent() - 1) * page.getSize()), (int) page.getSize());
        List<FlowTaskVO> flowList = new LinkedList<>();
        for (Task task : taskList) {
            FlowTaskVO flowTask = new FlowTaskVO();
            // 当前流程信息
            flowTask.setTaskId(task.getId());
            flowTask.setTaskDefKey(task.getTaskDefinitionKey());
            flowTask.setCreateTime(task.getCreateTime());
            flowTask.setProcDefId(task.getProcessDefinitionId());
            flowTask.setExecutionId(task.getExecutionId());
            flowTask.setTaskName(task.getName());

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(task.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            UserVO startUser = RetOps.of(remoteUserService.getDetails(Long.parseLong(historicProcessInstance.getStartUserId()))).getData().orElse(new UserVO());
            flowTask.setStartUserId(startUser.getUserId().toString());
            flowTask.setStartUserName(startUser.getName());
            flowTask.setStartDeptName(Optional.ofNullable(startUser.getDept()).orElse(new SysDept()).getName());
            flowList.add(flowTask);
        }

        Page<FlowTaskVO> result = new Page<>();
        result.setTotal(taskQuery.count());
        result.setRecords(flowList);
        return result;
    }

    @Override
    public IPage<FlowTaskVO> finishedList(Page page, FlowQueryDTO queryDto) {
        UserVO user = RetOps.of(remoteUserService.getDetails(SecurityUtils.getUser().getId())).getData().orElseThrow();

        HistoricTaskInstanceQuery taskInstanceQuery = historyService.createHistoricTaskInstanceQuery().includeProcessVariables()
                .finished()
                .taskAssignee(user.getUserId().toString())
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        List<HistoricTaskInstance> historicTaskInstanceList = taskInstanceQuery.listPage((int) ((page.getCurrent() - 1) * page.getSize()), (int) page.getSize());
        List<FlowTaskVO> hisTaskList = new ArrayList<>();
        for (HistoricTaskInstance histTask : historicTaskInstanceList) {
            FlowTaskVO flowTask = new FlowTaskVO();
            // 当前流程信息
            flowTask.setTaskId(histTask.getId());
            // 审批人员信息
            flowTask.setCreateTime(histTask.getCreateTime());
            flowTask.setFinishTime(histTask.getEndTime());
            flowTask.setDuration(getDateStr(histTask.getDurationInMillis()));
            flowTask.setProcDefId(histTask.getProcessDefinitionId());
            flowTask.setTaskName(histTask.getName());
            flowTask.setTaskDefKey(histTask.getTaskDefinitionKey());

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(histTask.getProcessDefinitionId())
                    .singleResult();
            flowTask.setDeployId(pd.getDeploymentId());
            flowTask.setProcDefName(pd.getName());
            flowTask.setProcDefVersion(pd.getVersion());
            flowTask.setProcInsId(histTask.getProcessInstanceId());
            flowTask.setHisProcInsId(histTask.getProcessInstanceId());

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(histTask.getProcessInstanceId())
                    .singleResult();
            UserVO startUser = RetOps.of(remoteUserService.getDetails(Long.parseLong(historicProcessInstance.getStartUserId()))).getData().orElse(new UserVO());
            flowTask.setStartUserId(startUser.getUserId().toString());
            flowTask.setStartDeptName(Optional.ofNullable(startUser.getDept()).orElse(new SysDept()).getName());
            flowTask.setStartUserName(startUser.getName());
            hisTaskList.add(flowTask);
        }
        Page<FlowTaskVO> result = new Page<>();
        result.setTotal(taskInstanceQuery.count());
        result.setRecords(hisTaskList);
        return result;
    }

    @Override
    public Boolean complete(FlowTaskDTO flowTaskDto) {
        Task task = taskService.createTaskQuery().taskId(flowTaskDto.getTaskId()).singleResult();
        if (ObjectUtil.isNull(task)) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_NULL));
        }
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            taskService.addComment(flowTaskDto.getTaskId(), flowTaskDto.getInstanceId(), FlowCommentEnums.DELEGATE.getType(), flowTaskDto.getComment());
            taskService.resolveTask(flowTaskDto.getTaskId(), flowTaskDto.getVariables());
        } else {
            taskService.addComment(flowTaskDto.getTaskId(), flowTaskDto.getInstanceId(), FlowCommentEnums.NORMAL.getType(), flowTaskDto.getComment());
            taskService.setAssignee(flowTaskDto.getTaskId(), SecurityUtils.getUser().getId().toString());
            taskService.complete(flowTaskDto.getTaskId(), flowTaskDto.getVariables());
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean taskReject(FlowTaskDTO flowTaskDto) {
        if (taskService.createTaskQuery().taskId(flowTaskDto.getTaskId()).singleResult().isSuspended()) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_HANG_UP));
        }

        // 当前任务 task
        Task task = taskService.createTaskQuery()
                .taskId(flowTaskDto.getTaskId())
                .singleResult();

        // 获取流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();

        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 类型为用户节点
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    // 获取节点信息
                    source = flowElement;
                }
            }
        }

        // 目的获取所有跳转到的节点 targetIds
        // 获取当前节点的所有父级用户任务节点
        // 深度优先算法思想：延边迭代深入
        List<UserTask> parentUserTaskList = FlowableUtils.iteratorFindParentUserTasks(source, null, null);
        if (parentUserTaskList == null || parentUserTaskList.size() == 0) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_FIRST));
        }
        // 获取活动 ID 即节点 Key
        List<String> parentUserTaskKeyList = new ArrayList<>();
        parentUserTaskList.forEach(item -> parentUserTaskKeyList.add(item.getId()));
        // 获取全部历史节点活动实例，即已经走过的节点历史，数据采用开始时间升序
        List<HistoricTaskInstance> historicTaskInstanceList = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId()).orderByHistoricTaskInstanceStartTime().asc().list();
        // 数据清洗，将回滚导致的脏数据清洗掉
        List<String> lastHistoricTaskInstanceList = FlowableUtils.historicTaskInstanceClean(allElements, historicTaskInstanceList);
        // 此时历史任务实例为倒序，获取最后走的节点
        List<String> targetIds = new ArrayList<>();
        // 循环结束标识，遇到当前目标节点的次数
        int number = 0;
        StringBuilder parentHistoricTaskKey = new StringBuilder();
        for (String historicTaskInstanceKey : lastHistoricTaskInstanceList) {
            // 当会签时候会出现特殊的，连续都是同一个节点历史数据的情况，这种时候跳过
            if (parentHistoricTaskKey.toString().equals(historicTaskInstanceKey)) {
                continue;
            }
            parentHistoricTaskKey = new StringBuilder(historicTaskInstanceKey);
            if (historicTaskInstanceKey.equals(task.getTaskDefinitionKey())) {
                number++;
            }
            // 在数据清洗后，历史节点就是唯一一条从起始到当前节点的历史记录，理论上每个点只会出现一次
            // 在流程中如果出现循环，那么每次循环中间的点也只会出现一次，再出现就是下次循环
            // number == 1，第一次遇到当前节点
            // number == 2，第二次遇到，代表最后一次的循环范围
            if (number == 2) {
                break;
            }
            // 如果当前历史节点，属于父级的节点，说明最后一次经过了这个点，需要退回这个点
            if (parentUserTaskKeyList.contains(historicTaskInstanceKey)) {
                targetIds.add(historicTaskInstanceKey);
            }
        }


        // 目的获取所有需要被跳转的节点 currentIds
        // 取其中一个父级任务，因为后续要么存在公共网关，要么就是串行公共线路
        UserTask oneUserTask = parentUserTaskList.get(0);
        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需驳回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(oneUserTask, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));


        // 规定：并行网关之前节点必须需存在唯一用户任务节点，如果出现多个任务节点，则并行网关节点默认为结束节点，原因为不考虑多对多情况
        if (targetIds.size() > 1 && currentIds.size() > 1) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_MANY));
        }

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置驳回意见
        currentTaskIds.forEach(item -> taskService.addComment(item, task.getProcessInstanceId(), FlowCommentEnums.REJECT.getType(), flowTaskDto.getComment()));

        try {
            // 如果父级任务多于 1 个，说明当前节点不是并行节点，原因为不考虑多对多情况
            if (targetIds.size() > 1) {
                // 1 对 多任务跳转，currentIds 当前节点(1)，targetIds 跳转到的节点(多)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId()).
                        moveSingleActivityIdToActivityIds(currentIds.get(0), targetIds).changeState();
            }
            // 如果父级任务只有一个，因此当前任务可能为网关中的任务
            if (targetIds.size() == 1) {
                // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetIds.get(0) 跳转到的节点(1)
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId())
                        .moveActivityIdsToSingleActivityId(currentIds, targetIds.get(0)).changeState();
            }
        } catch (FlowableObjectNotFoundException e) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_FLOW_NULL));
        } catch (FlowableException e) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_ERROR));
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopProcess(FlowTaskDTO flowTaskDto) {
        List<Task> task = taskService.createTaskQuery().processInstanceId(flowTaskDto.getInstanceId()).list();
        if (CollectionUtils.isEmpty(task)) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowDefinitionConstant.ERROR_START));
        }

        // 获取当前流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(flowTaskDto.getInstanceId())
                .singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        if (Objects.nonNull(bpmnModel)) {
            Process process = bpmnModel.getMainProcess();
            List<EndEvent> endNodes = process.findFlowElementsOfType(EndEvent.class, false);
            if (CollectionUtils.isNotEmpty(endNodes)) {
                // 获取当前流程最后一个节点
                String endId = endNodes.get(0).getId();
                List<Execution> executions = runtimeService.createExecutionQuery()
                        .parentId(processInstance.getProcessInstanceId()).list();
                List<String> executionIds = new ArrayList<>();
                executions.forEach(execution -> executionIds.add(execution.getId()));
                // 变更流程为已结束状态
                runtimeService.createChangeActivityStateBuilder()
                        .moveExecutionsToSingleActivityId(executionIds, endId).changeState();
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public FlowNextVO getNextFlowNodeByStart(FlowTaskDTO flowTaskDto) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(flowTaskDto.getDeploymentId())
                .singleResult();

        List<UserTask> nextUserTask = FindNextNodeUtil.getNextUserTasksByStart(repositoryService, processDefinition, flowTaskDto.getVariables());
        if (CollectionUtils.isEmpty(nextUserTask)) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_NULL_NEXT));
        }
        return getFlowAttribute(nextUserTask);
    }

    @Override
    public FlowNextVO getNextFlowNode(FlowTaskDTO flowTaskDto) {
        Task task = taskService.createTaskQuery().taskId(flowTaskDto.getTaskId()).singleResult();
        if (Objects.isNull(task)) {
            throw new FlowErrorException(MsgUtils.getMessage(FlowTaskConstant.ERROR_TASK_NULL_OR_USE));
        }
        //  获取当前流程所有流程变量(网关节点时需要校验表达式)
        Map<String, Object> variables = taskService.getVariables(task.getId());
        List<UserTask> nextUserTask = FindNextNodeUtil.getNextUserTasks(repositoryService, task, variables);
        if (CollectionUtils.isEmpty(nextUserTask)) {
           return new FlowNextVO();
        }
        return getFlowAttribute(nextUserTask);
    }

    @Override
    public Map<String, Object> processVariables(String taskId) {
        // 流程变量
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                .includeProcessVariables()
                .finished()
                .taskId(taskId)
                .singleResult();
        if (Objects.nonNull(historicTaskInstance)) {
            return historicTaskInstance.getProcessVariables();
        } else {
            return taskService.getVariables(taskId);
        }
    }

    @Override
    public Map<String, Object> flowRecord(String procInsId, String deployId) {
        Map<String, Object> result = new HashMap<>(16);
        if (StringUtils.isNotBlank(procInsId)) {
            List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(procInsId)
                    .orderByHistoricActivityInstanceStartTime()
                    .desc().list();

            List<FlowTaskVO> hisFlowList = new ArrayList<>();
            for (HistoricActivityInstance histIns : list) {
                if (StringUtils.isBlank(histIns.getTaskId())) {
                    continue;
                }

                FlowTaskVO flowTask = new FlowTaskVO();
                flowTask.setTaskId(histIns.getTaskId());
                flowTask.setTaskName(histIns.getActivityName());
                flowTask.setCreateTime(histIns.getStartTime());
                flowTask.setFinishTime(histIns.getEndTime());
                if (StringUtils.isNotBlank(histIns.getAssignee())) {
                    UserVO userVo = RetOps.of(remoteUserService.getDetails(Long.parseLong(histIns.getAssignee()))).getData().orElse(new UserVO());
                    flowTask.setAssigneeId(userVo.getUserId());
                    flowTask.setAssigneeName(userVo.getName());
                    flowTask.setDeptName(Optional.ofNullable(userVo.getDept()).orElse(new SysDept()).getName());
                }
                // 展示审批人员
                List<HistoricIdentityLink> linksForTask = historyService.getHistoricIdentityLinksForTask(histIns.getTaskId());
                StringBuilder stringBuilder = new StringBuilder();
                for (HistoricIdentityLink identityLink : linksForTask) {
                    // 获选人,候选组/角色(多个)
                    if ("candidate".equals(identityLink.getType())) {
                        if (StringUtils.isNotBlank(identityLink.getUserId())) {
                            UserVO userVo = RetOps.of(remoteUserService.getDetails(Long.parseLong(identityLink.getUserId()))).getData().orElse(new UserVO());
                            stringBuilder.append(userVo.getName()).append(",");
                        }
                        if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                            SysRole sysRole = new SysRole();
                            sysRole.setRoleId(Long.parseLong(identityLink.getGroupId()));
                            List<SysRole> sysRoles = RetOps.of(remoteRoleService.getDetails(sysRole)).getData().orElse(Collections.emptyList());
                            for (SysRole role : sysRoles) {
                                stringBuilder.append(role.getRoleName()).append(",");
                            }
                        }
                    }
                }
                if (StringUtils.isNotBlank(stringBuilder)) {
                    flowTask.setCandidate(stringBuilder.substring(0, stringBuilder.length() - 1));
                }

                flowTask.setDuration(histIns.getDurationInMillis() == null || histIns.getDurationInMillis() == 0 ? null : getDateStr(histIns.getDurationInMillis()));
                // 获取意见评论内容
                List<Comment> commentList = taskService.getProcessInstanceComments(histIns.getProcessInstanceId());
                commentList.forEach(comment -> {
                    if (histIns.getTaskId().equals(comment.getTaskId())) {
                        flowTask.setComment(FlowCommentVO.builder().type(comment.getType()).comment(comment.getFullMessage()).build());
                    }
                });
                hisFlowList.add(flowTask);
            }
            result.put("flowList", hisFlowList);
        }
        return result;
    }


    /**
     * 获取任务节点属性,包含自定义属性等
     *
     * @param nextUserTask nextUserTask
     * @return nextVo
     */
    private FlowNextVO getFlowAttribute(List<UserTask> nextUserTask) {
        FlowNextVO flowNextVo = new FlowNextVO();

        for (UserTask userTask : nextUserTask) {
            MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
            // 会签节点
            if (Objects.nonNull(multiInstance)) {
                flowNextVo.setVars(multiInstance.getInputDataItem());
                flowNextVo.setType(FlowDefinitionConstant.PROCESS_MULTI_INSTANCE);
                flowNextVo.setDataType(FlowDefinitionConstant.DYNAMIC);
            } else {
                // 读取自定义节点属性 判断是否是否需要动态指定任务接收人员、组
                String dataType = userTask.getAttributeValue(FlowDefinitionConstant.NAMASPASE, FlowDefinitionConstant.PROCESS_CUSTOM_DATA_TYPE);
                String userType = userTask.getAttributeValue(FlowDefinitionConstant.NAMASPASE, FlowDefinitionConstant.PROCESS_CUSTOM_USER_TYPE);
                flowNextVo.setVars(FlowDefinitionConstant.PROCESS_APPROVAL);
                flowNextVo.setType(userType);
                flowNextVo.setDataType(dataType);
            }
        }
        return flowNextVo;
    }

    /**
     * 将毫秒数转换为中文时间字符串（天/小时/分钟/秒）。
     *
     * @param ms 毫秒值
     * @return str
     */
    private String getDateStr(long ms) {
        long day = ms / (24 * 60 * 60 * 1000);
        long hour = (ms / (60 * 60 * 1000) - day * 24);
        long minute = ((ms / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long second = (ms / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - minute * 60);

        if (day > 0) {
            return day + "天" + hour + "小时" + minute + "分钟";
        }
        if (hour > 0) {
            return hour + "小时" + minute + "分钟";
        }
        if (minute > 0) {
            return minute + "分钟";
        }
        if (second > 0) {
            return second + "秒";
        } else {
            return 0 + "秒";
        }
    }
}
