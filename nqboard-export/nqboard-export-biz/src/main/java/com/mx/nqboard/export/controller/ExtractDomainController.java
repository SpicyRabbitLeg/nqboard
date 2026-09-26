package com.mx.nqboard.export.controller;

import cn.hutool.core.collection.CollUtil;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.export.api.entity.ExtractDomainEntity;
import com.mx.nqboard.export.service.ExtractDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 专家抽取领域管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/extract/domain")
@Tag(description = "extractDomain", name = "专家抽取领域管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ExtractDomainController {

	private final ExtractDomainService extractDomainService;

	/**
	 * 领域清单列表
	 * @param status 状态（可选）
	 * @return 领域列表
	 */
	@Operation(summary = "领域清单列表", description = "领域清单列表")
	@GetMapping("/list")
	public R listDomains(@RequestParam(required = false) String status) {
		return R.ok(extractDomainService.listDomains(status));
	}

	/**
	 * 新增领域
	 * @param domain 领域实体
	 * @return R
	 */
	@Operation(summary = "新增领域", description = "新增领域")
	@SysLog("新增专家抽取领域")
	@PostMapping
	@HasPermission("export_extract_domain_add")
	public R saveDomain(@Validated @RequestBody ExtractDomainEntity domain) {
		return R.ok(extractDomainService.saveDomain(domain));
	}

	/**
	 * 修改领域
	 * @param domain 领域实体
	 * @return R
	 */
	@Operation(summary = "修改领域", description = "修改领域")
	@SysLog("修改专家抽取领域")
	@PutMapping
	@HasPermission("export_extract_domain_edit")
	public R updateDomain(@Validated @RequestBody ExtractDomainEntity domain) {
		return R.ok(extractDomainService.updateDomain(domain));
	}

	/**
	 * 删除领域
	 * @param ids 领域id列表
	 * @return R
	 */
	@Operation(summary = "删除领域", description = "删除领域")
	@SysLog("删除专家抽取领域")
	@DeleteMapping
	@HasPermission("export_extract_domain_del")
	public R removeDomains(@RequestBody Long[] ids) {
		return R.ok(extractDomainService.removeDomains(ids));
	}

	/**
	 * 触发 AI 归纳领域清单草稿（异步执行，进度经 /generate/progress 查询，结果经 /generate/result 获取）
	 * @return R
	 */
	@Operation(summary = "触发AI归纳领域清单草稿", description = "触发AI归纳领域清单草稿（异步执行，防并发重入）")
	@SysLog("AI归纳专家抽取领域清单")
	@PostMapping("/generate")
	@HasPermission("export_extract_domain_edit")
	public R generateDraft() {
		extractDomainService.startGenerate();
		return R.ok("AI 归纳任务已启动");
	}

	/**
	 * AI 归纳任务进度
	 * @return running/done/total/error
	 */
	@Operation(summary = "AI归纳任务进度", description = "AI归纳任务进度（running/done/total/error）")
	@GetMapping("/generate/progress")
	public R generateProgress() {
		return R.ok(extractDomainService.getGenerateProgress());
	}

	/**
	 * AI 归纳领域草稿结果（任务完成后调用）
	 * @return 领域草稿列表
	 */
	@Operation(summary = "AI归纳领域草稿结果", description = "AI归纳领域草稿结果（任务完成后返回）")
	@GetMapping("/generate/result")
	public R generateResult() {
		return R.ok(extractDomainService.getGenerateResult());
	}

	/**
	 * 批量保存领域草稿
	 * @param domains 领域草稿列表
	 * @return R
	 */
	@Operation(summary = "批量保存领域草稿", description = "批量保存领域草稿")
	@SysLog("批量保存专家抽取领域")
	@PostMapping("/batch")
	@HasPermission("export_extract_domain_add")
	public R saveBatchDomains(@RequestBody List<ExtractDomainEntity> domains) {
		return R.ok(extractDomainService.saveBatchDomains(domains));
	}

	/**
	 * 触发全量领域打标（异步）
	 * @return R
	 */
	@Operation(summary = "触发领域打标", description = "触发全量领域打标（异步执行）")
	@SysLog("触发专家领域打标")
	@PostMapping("/tagging")
	@HasPermission("export_extract_domain_edit")
	public R startTagging() {
		extractDomainService.startTagging();
		return R.ok("打标任务已启动");
	}

	/**
	 * 打标进度
	 * @return running/done/total
	 */
	@Operation(summary = "打标进度", description = "打标进度")
	@GetMapping("/tagging/progress")
	public R taggingProgress() {
		return R.ok(extractDomainService.getTaggingProgress());
	}
}
