package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.export.api.dto.ExtractRunDTO;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;

/**
 * <p>
 * 专家抽取执行 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExtractRunService {

	/**
	 * 执行抽取：意图解析 -> 已选(领域精确命中) -> 候选(邻接领域+向量种子) -> 未匹配动态计算 -> 落库
	 * @param dto 抽取请求
	 * @return 抽取记录（含三档数量统计）
	 */
	ExtractRecordEntity run(ExtractRunDTO dto);

	/**
	 * 未匹配专家动态分页（不入库，按记录的解析领域与候选名单实时反查，先查 id 再查详情）
	 * @param page 分页对象
	 * @param recordId 抽取记录id
	 * @return 未匹配专家分页
	 */
	Page<ExpertEntity> pageUnmatched(Page<ExpertEntity> page, Long recordId);
}
