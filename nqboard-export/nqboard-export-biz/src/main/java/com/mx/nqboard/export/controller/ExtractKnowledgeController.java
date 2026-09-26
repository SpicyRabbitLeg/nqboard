package com.mx.nqboard.export.controller;

import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.export.service.ExtractKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 专家知识库同步 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/extract/knowledge")
@Tag(description = "extractKnowledge", name = "专家知识库同步")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ExtractKnowledgeController {

	private final ExtractKnowledgeService extractKnowledgeService;

	/**
	 * 触发全量专家画像同步至 Dify 知识库（异步）
	 * @return R
	 */
	@Operation(summary = "同步专家知识库", description = "全量专家画像同步至 Dify 知识库（先清空后重建，异步执行）")
	@SysLog("同步专家知识库")
	@PostMapping("/sync")
	@HasPermission("export_extract_domain_edit")
	public R startSync() {
		return R.ok(extractKnowledgeService.startSync());
	}

	/**
	 * 同步进度（分片文件维度）
	 * @return running/done/total
	 */
	@Operation(summary = "同步进度", description = "同步进度")
	@GetMapping("/progress")
	public R progress() {
		return R.ok(extractKnowledgeService.getProgress());
	}

	/**
	 * Dify 侧向量索引状态
	 * @return completed/total/ready
	 */
	@Operation(summary = "向量索引状态", description = "Dify 侧向量索引状态")
	@GetMapping("/indexing-status")
	public R indexingStatus() {
		return R.ok(extractKnowledgeService.getIndexingStatus());
	}
}
