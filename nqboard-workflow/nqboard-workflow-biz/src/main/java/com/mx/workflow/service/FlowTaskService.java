package com.mx.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.workflow.api.dto.FlowQueryDTO;
import com.mx.workflow.api.dto.FlowTaskDTO;
import com.mx.workflow.api.vo.FlowNextVO;
import com.mx.workflow.api.vo.FlowTaskVO;

import java.util.Map;

/**
 * <p>
 * 工作流流程任务管理 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface FlowTaskService {

    /**
     * 流程节点信息
     *
     * @param procInsId 实例id
     * @param deployId  部署id
     * @return map
     */
    Map<String, Object> flowXmlAndNode(String procInsId, String deployId);

    /**
     * 我发起列表
     *
     * @param page        page
     * @param flowQueryDto flowQueryDto
     * @return page
     */
    IPage<FlowTaskVO> myProcess(Page page, FlowQueryDTO flowQueryDto);

    /**
     * 待办列表
     *
     * @param page        page
     * @param flowQueryDto flowQueryDto
     * @return page
     */
    IPage<FlowTaskVO> todoList(Page page, FlowQueryDTO flowQueryDto);

    /**
     * 已办列表
     * @param page        page
     * @param queryDto flowQueryDto
     * @return page
     */
    IPage<FlowTaskVO> finishedList(Page page,FlowQueryDTO queryDto);

    /**
     * 审批任务
     *
     * @param flowTaskDto flowTaskDto
     * @return 成功否
     */
    Boolean complete(FlowTaskDTO flowTaskDto);

    /**
     * 驳回任务
     *
     * @param flowTaskDto flowTaskDto
     * @return 是否成功
     */
    Boolean taskReject(FlowTaskDTO flowTaskDto);

    /**
     * 取消申请
     * @param flowTaskDto flowTaskDto
     * @return 成功否
     */
    Boolean stopProcess(FlowTaskDTO flowTaskDto);

    /**
     * 流程发起时获取下一节点
     * @param flowTaskDto flowTaskDto
     * @return nextVo
     */
    FlowNextVO getNextFlowNodeByStart(FlowTaskDTO flowTaskDto);

    /**
     * 获取下一节点
     *
     * @param flowTaskDto flowTaskDto
     * @return nextVo
     */
    FlowNextVO getNextFlowNode(FlowTaskDTO flowTaskDto);

    /**
     * 获取流程变量
     * @param taskId 流程任务Id
     * @return map
     */
    Map<String,Object> processVariables(String taskId);

    /**
     *流程历史流转记录
     * @param procInsId procInsId
     * @param deployId deployId
     * @return map
     */
    Map<String, Object> flowRecord(String procInsId, String deployId);
}
