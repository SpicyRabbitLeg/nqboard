package com.mx.nqboard.export.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.vo.ExpertExcelVO;
import com.mx.nqboard.export.api.vo.ExpertExportVO;
import com.mx.nqboard.export.service.ExpertService;
import com.pig4cloud.plugin.excel.annotation.RequestExcel;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 专家管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/expert")
@Tag(description = "expert", name = "专家管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ExpertController {

	private final ExpertService expertService;

	/**
	 * 分页查询
	 * @param page 分页对象
	 * @param expert 专家表
	 * @return 分页对象
	 */
	@Operation(summary = "分页查询", description = "分页查询")
	@GetMapping("/page")
	public R getExpertPage(@ParameterObject Page<ExpertEntity> page, @ParameterObject ExpertEntity expert) {
		LambdaQueryWrapper<ExpertEntity> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(StrUtil.isNotBlank(expert.getSubjectCategory()), ExpertEntity::getSubjectCategory, expert.getSubjectCategory())
			.eq(StrUtil.isNotBlank(expert.getFirstDiscipline()), ExpertEntity::getFirstDiscipline, expert.getFirstDiscipline())
			.eq(StrUtil.isNotBlank(expert.getSecondDiscipline()), ExpertEntity::getSecondDiscipline, expert.getSecondDiscipline())
			.like(StrUtil.isNotBlank(expert.getResearchDirection()), ExpertEntity::getResearchDirection, expert.getResearchDirection());
		return R.ok(expertService.page(page, wrapper));
	}

	/**
	 * 通过条件查询专家表
	 * @param expert 查询条件
	 * @return R 对象列表
	 */
	@Operation(summary = "通过条件查询", description = "通过条件查询对象")
	@GetMapping("/details")
	public R getDetails(@ParameterObject ExpertEntity expert) {
		return R.ok(expertService.list(Wrappers.query(expert)));
	}

	/**
	 * 新增专家表
	 * @param expert 专家表
	 * @return R
	 */
	@Operation(summary = "新增专家表", description = "新增专家表")
	@SysLog("新增专家")
	@PostMapping
	@HasPermission("export_expert_add")
	public R save(@Validated @RequestBody ExpertEntity expert) {
		return R.ok(expertService.save(expert));
	}

	/**
	 * 修改专家表
	 * @param expert 专家表
	 * @return R
	 */
	@Operation(summary = "修改专家表", description = "修改专家表")
	@SysLog("修改专家")
	@PutMapping
	@HasPermission("export_expert_edit")
	public R updateById(@Validated @RequestBody ExpertEntity expert) {
		return R.ok(expertService.updateById(expert));
	}

	/**
	 * 导入专家信息
	 * @param excelVOList 专家Excel数据列表
	 * @param bindingResult 数据校验结果
	 * @return 导入结果
	 */
	@PostMapping("/import")
	@HasPermission("export_expert_add")
	@Operation(summary = "导入专家信息", description = "导入专家信息")
	public R importExpert(@RequestExcel List<ExpertExcelVO> excelVOList, BindingResult bindingResult) {
		return expertService.importExperts(excelVOList, bindingResult);
	}

	/**
	 * 通过id删除专家表
	 * @param ids id列表
	 * @return R
	 */
	@Operation(summary = "通过id删除专家表", description = "通过id删除专家表")
	@SysLog("通过id删除专家")
	@DeleteMapping
	@HasPermission("export_expert_del")
	public R removeById(@RequestBody Long[] ids) {
		return R.ok(expertService.removeBatchByIds(CollUtil.toList(ids)));
	}

	/**
	 * 导出excel 表格
	 * @param expert 查询条件
	 * @param ids 导出指定ID
	 * @return excel 文件流
	 */
	@Operation(summary = "导出excel 表格", description = "导出excel 表格")
	@ResponseExcel
	@GetMapping("/export")
	@HasPermission("export_expert_export")
	public List<ExpertExportVO> exportExcel(ExpertEntity expert, Long[] ids) {
		return BeanUtil.copyToList(
				expertService.list(Wrappers.lambdaQuery(expert).in(ArrayUtil.isNotEmpty(ids), ExpertEntity::getId, ids)),
				ExpertExportVO.class);
	}
}
