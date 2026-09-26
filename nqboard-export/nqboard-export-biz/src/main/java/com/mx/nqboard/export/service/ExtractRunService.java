package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.export.api.dto.ExtractRunDTO;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;

import java.util.Map;

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
	 * 执行抽取（异步启动）：预建运行中记录后立即返回，任务经 意图解析 -> 池构建 -> 向量打分 -> 分档复核 -> 落库
	 * @param dto 抽取请求
	 * @return 运行中的抽取记录（status=2，进度见 progress）
	 */
	ExtractRecordEntity run(ExtractRunDTO dto);

	/**
	 * 当前抽取任务进度（单飞，同一时刻至多一个任务）
	 * @return running/stage/done/total/recordId/error
	 */
	Map<String, Object> progress();

	/**
	 * 未匹配专家动态分页（不入库，按记录的解析领域与存活候选名单实时反查，含 LLM 复核剔除者）
	 * @param page 分页对象
	 * @param recordId 抽取记录id
	 * @return 未匹配专家分页
	 */
	Page<ExpertEntity> pageUnmatched(Page<ExpertEntity> page, Long recordId);
}
