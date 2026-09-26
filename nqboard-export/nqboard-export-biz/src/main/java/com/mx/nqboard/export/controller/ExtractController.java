package com.mx.nqboard.export.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.export.api.dto.ExtractRunDTO;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordDetailEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;
import com.mx.nqboard.export.service.ExtractRecordDetailService;
import com.mx.nqboard.export.service.ExtractRecordService;
import com.mx.nqboard.export.service.ExtractRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 专家抽取 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/extract")
@Tag(description = "extract", name = "专家抽取")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ExtractController {

	private final ExtractRunService extractRunService;

	private final ExtractRecordService extractRecordService;

	private final ExtractRecordDetailService extractRecordDetailService;

	/**
	 * 执行专家抽取
	 * @param dto 抽取请求
	 * @return 抽取记录（含三档数量统计）
	 */
	@Operation(summary = "执行专家抽取", description = "自然语言抽取专家：已选/候选/未匹配三档")
	@SysLog("专家抽取")
	@PostMapping("/run")
	@HasPermission("export_extract_run")
	public R run(@Validated @RequestBody ExtractRunDTO dto) {
		return R.ok(extractRunService.run(dto));
	}

	/**
	 * 抽取历史记录分页
	 * @param page 分页对象
	 * @param keyword 查询内容关键词（可选）
	 * @return 历史记录分页
	 */
	@Operation(summary = "抽取历史记录分页", description = "抽取历史记录分页")
	@GetMapping("/record/page")
	public R pageRecords(@ParameterObject Page<ExtractRecordEntity> page,
			@RequestParam(required = false) String keyword) {
		return R.ok(extractRecordService.pageRecords(page, keyword));
	}

	/**
	 * 已选/候选明细分页
	 * @param recordId 抽取记录id
	 * @param page 分页对象
	 * @param grade 档位（1已选、2候选）
	 * @return 明细分页
	 */
	@Operation(summary = "抽取记录明细分页", description = "已选/候选明细分页")
	@GetMapping("/record/{recordId}/detail")
	public R pageDetails(@PathVariable Long recordId, @ParameterObject Page<ExtractRecordDetailEntity> page,
			@RequestParam String grade) {
		return R.ok(extractRecordDetailService.pageDetails(page, recordId, grade));
	}

	/**
	 * 未匹配专家动态分页
	 * @param recordId 抽取记录id
	 * @param page 分页对象
	 * @return 未匹配专家分页
	 */
	@Operation(summary = "未匹配专家动态分页", description = "按记录动态反查未匹配专家（不入库）")
	@GetMapping("/record/{recordId}/unmatched")
	public R pageUnmatched(@PathVariable Long recordId, @ParameterObject Page<ExpertEntity> page) {
		return R.ok(extractRunService.pageUnmatched(page, recordId));
	}
}
