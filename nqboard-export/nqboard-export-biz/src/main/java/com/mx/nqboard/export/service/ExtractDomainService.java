package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.export.api.entity.ExtractDomainEntity;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 专家抽取领域管理 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExtractDomainService extends IService<ExtractDomainEntity> {

	/**
	 * 领域清单列表
	 * @param status 状态（可选）
	 * @return 领域列表
	 */
	List<ExtractDomainEntity> listDomains(String status);

	/**
	 * 新增领域（编码查重）
	 * @param domain 领域实体
	 * @return 是否成功
	 */
	Boolean saveDomain(ExtractDomainEntity domain);

	/**
	 * 修改领域（编码查重）
	 * @param domain 领域实体
	 * @return 是否成功
	 */
	Boolean updateDomain(ExtractDomainEntity domain);

	/**
	 * 批量删除领域
	 * @param ids 领域id列表
	 * @return 是否成功
	 */
	Boolean removeDomains(Long[] ids);

	/**
	 * 触发 AI 归纳领域清单草稿（异步执行，不落库；进度经 getGenerateProgress 查询，结果经 getGenerateResult 获取）
	 */
	void startGenerate();

	/**
	 * AI 归纳任务进度
	 * @return running/done/total/error
	 */
	Map<String, Object> getGenerateProgress();

	/**
	 * AI 归纳领域草稿结果（任务完成后返回，结果保留至下次触发）
	 * @return 领域草稿列表
	 */
	List<ExtractDomainEntity> getGenerateResult();

	/**
	 * 批量保存领域草稿（逐条编码查重）
	 * @param domains 领域草稿列表
	 * @return 是否成功
	 */
	Boolean saveBatchDomains(List<ExtractDomainEntity> domains);

	/**
	 * 触发全量领域打标（异步执行，按 distinct 研究方向组合分批调用 Dify）
	 */
	void startTagging();

	/**
	 * 打标进度
	 * @return running/done/total
	 */
	Map<String, Object> getTaggingProgress();
}
