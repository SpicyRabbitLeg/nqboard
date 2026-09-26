package com.mx.nqboard.export.service;

import java.util.Map;

/**
 * <p>
 * 专家知识库同步 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExtractKnowledgeService {

	/**
	 * 触发全量专家画像同步至 Dify 知识库（异步执行，先清空后重建）
	 * @return 启动结果信息
	 */
	Map<String, Object> startSync();

	/**
	 * 同步进度（单位：分片文件）
	 * @return running/done/total
	 */
	Map<String, Object> getProgress();

	/**
	 * Dify 侧向量化索引状态
	 * @return completed/total/ready
	 */
	Map<String, Object> getIndexingStatus();
}
