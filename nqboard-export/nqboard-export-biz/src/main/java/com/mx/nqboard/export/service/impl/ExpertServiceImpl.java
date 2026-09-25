package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.vo.ExpertExcelVO;
import com.mx.nqboard.export.mapper.ExpertMapper;
import com.mx.nqboard.export.service.ExpertService;
import com.pig4cloud.plugin.excel.vo.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertServiceImpl extends ServiceImpl<ExpertMapper, ExpertEntity> implements ExpertService {

	private final ExpertMapper expertMapper;

	@Override
	public Page<ExpertEntity> pageExperts(Page<ExpertEntity> page, ExpertEntity query) {
		// 第一步：仅分页查询 id（覆盖索引，避免深分页回表）
		LambdaQueryWrapper<ExpertEntity> idWrapper = buildQueryWrapper(query).select(ExpertEntity::getId);
		if (CollUtil.isEmpty(page.orders())) {
			// 无用户排序时默认按 id 倒序，保证分页稳定
			idWrapper.orderByDesc(ExpertEntity::getId);
		}
		Page<ExpertEntity> idPage = this.page(page, idWrapper);
		List<Long> ids = idPage.getRecords().stream().map(ExpertEntity::getId).toList();
		if (CollUtil.isEmpty(ids)) {
			idPage.setRecords(Collections.emptyList());
			return idPage;
		}
		// 第二步：按 id in 批量查详情
		Map<Long, ExpertEntity> detailMap = this.listByIds(ids).stream()
			.collect(Collectors.toMap(ExpertEntity::getId, Function.identity()));
		// 第三步：IN 不保序，按 id 页顺序回填，保证与分页排序一致
		idPage.setRecords(ids.stream().map(detailMap::get).filter(Objects::nonNull).toList());
		return idPage;
	}

	@Override
	public List<String> listCategoryOptions() {
		return listObjs(Wrappers.<ExpertEntity>query()
			.select("DISTINCT subject_category")
			.isNotNull("subject_category")
			.ne("subject_category", "")
			.orderByAsc("subject_category"))
			.stream()
			.map(String::valueOf)
			.toList();
	}

	@Override
	public List<String> listDisciplineOptions(String category) {
		return listObjs(Wrappers.<ExpertEntity>query()
			.select("DISTINCT first_discipline")
			.isNotNull("first_discipline")
			.ne("first_discipline", "")
			.eq(StrUtil.isNotBlank(category), "subject_category", category)
			.orderByAsc("first_discipline"))
			.stream()
			.map(String::valueOf)
			.toList();
	}

	/**
	 * 组装列表/分页共用的查询条件（保证 id 查询与详情查询口径一致）
	 * @param query 查询条件
	 * @return 查询构造器
	 */
	private LambdaQueryWrapper<ExpertEntity> buildQueryWrapper(ExpertEntity query) {
		return Wrappers.<ExpertEntity>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getExpertName()), ExpertEntity::getExpertName, query.getExpertName())
			.eq(StrUtil.isNotBlank(query.getSubjectCategory()), ExpertEntity::getSubjectCategory, query.getSubjectCategory())
			.eq(StrUtil.isNotBlank(query.getFirstDiscipline()), ExpertEntity::getFirstDiscipline, query.getFirstDiscipline())
			.eq(StrUtil.isNotBlank(query.getSecondDiscipline()), ExpertEntity::getSecondDiscipline, query.getSecondDiscipline())
			.like(StrUtil.isNotBlank(query.getResearchDirection()), ExpertEntity::getResearchDirection, query.getResearchDirection());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R importExperts(List<ExpertExcelVO> excelVOList, BindingResult bindingResult) {
		// 通用校验获取失败的数据（@RequestExcel 按 ExcelVO 上的 JSR380 注解校验后的错误行）
		@SuppressWarnings("unchecked")
		List<ErrorMessage> errorMessageList = (List<ErrorMessage>) bindingResult.getTarget();

		// 一次性加载库中已有专家的 名称|门类|一级学科 组合用于查重，避免逐行查库
		Set<String> existingKeys = this.list().stream()
			.map(e -> StrUtil.format("{}|{}|{}", e.getExpertName(), e.getSubjectCategory(), e.getFirstDiscipline()))
			.collect(Collectors.toSet());

		List<ExpertEntity> saveList = new ArrayList<>();
		for (ExpertExcelVO excel : excelVOList) {
			// 必填字段兜底校验（空值行不落库，进入错误清单）
			if (StrUtil.hasBlank(excel.getExpertName(), excel.getSubjectCategory(), excel.getFirstDiscipline(),
					excel.getResearchDirection())) {
				errorMessageList.add(new ErrorMessage(excel.getLineNum(), Set.of("必填字段存在空值")));
				continue;
			}

			Set<String> errorMsg = new HashSet<>();
			// 同名同学科专家查重（含本批次内重复）
			String key = StrUtil.format("{}|{}|{}", excel.getExpertName(), excel.getSubjectCategory(),
					excel.getFirstDiscipline());
			if (!existingKeys.add(key)) {
				errorMsg.add("专家已存在（专家名称+学科门类+一级学科 重复）");
			}

			if (CollUtil.isEmpty(errorMsg)) {
				ExpertEntity entity = new ExpertEntity();
				entity.setExpertName(excel.getExpertName());
				entity.setSubjectCategory(excel.getSubjectCategory());
				entity.setFirstDiscipline(excel.getFirstDiscipline());
				entity.setResearchDirection(excel.getResearchDirection());
				saveList.add(entity);
			}
			else {
				errorMessageList.add(new ErrorMessage(excel.getLineNum(), errorMsg));
			}
		}

		if (CollUtil.isNotEmpty(errorMessageList)) {
			return R.failed(errorMessageList);
		}
		this.saveBatch(saveList);
		return R.ok(saveList.size());
	}
}
