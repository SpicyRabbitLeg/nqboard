package com.mx.nqboard.workflow.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.workflow.api.vo.FlowProcDefVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程定义查询
 *
 * @author 泥鳅压滑板
 */
@Mapper
public interface FlowDeployMapper {

    /**
     * 流程定义列表
     *
     * @param page page
     * @param name 流程名称
     * @return page
     */
    IPage<FlowProcDefVO> selectDeployList(Page page, String name);
}
