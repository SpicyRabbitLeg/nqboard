package com.mx.nqboard.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.workflow.api.dto.FlowDefinitionDTO;
import com.mx.nqboard.workflow.api.dto.FlowSaveXmlDTO;
import com.mx.nqboard.workflow.api.dto.FlowStartDTO;
import com.mx.nqboard.workflow.api.vo.FlowProcDefVO;

import java.util.List;

/**
 * <p>
 * 流程定义管理 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface FlowDefinitionService {

    /**
     * 分页查询
     *
     * @param page              分页对象
     * @param flowDefinitionDto 流程对象
     * @return page
     */
    IPage<FlowProcDefVO> getFlowDefinitionPage(Page page, FlowDefinitionDTO flowDefinitionDto);

    /**
     * 导入流程文件
     * 当每个key的流程第一次部署时，指定版本为1。对其后所有使用相同key的流程定义，
     * 部署时版本会在该key当前已部署的最高版本号基础上加1。key参数用于区分流程定义
     *
     * @param flowSaveXmlDto flowSaveXmlDto
     * @return 成功否
     */
    Boolean importFile(FlowSaveXmlDTO flowSaveXmlDto);

    /**
     * 通过id删除删除流程表
     *
     * @param deployIds deployIds
     */
    void deleteById(List<String> deployIds);

    /**
     * 激活或挂起流程定义
     *
     * @param flowDefinitionDto flowDefinitionDto
     */
    void updateState(FlowDefinitionDTO flowDefinitionDto);

    /**
     * 获取xml文件
     *
     * @param deployId 部署id
     * @return 文件字符串
     */
    String getXml(String deployId);

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param flowStartDto flowStartDto
     * @return x
     */
    Boolean startProcessInstanceById(FlowStartDTO flowStartDto);
}
