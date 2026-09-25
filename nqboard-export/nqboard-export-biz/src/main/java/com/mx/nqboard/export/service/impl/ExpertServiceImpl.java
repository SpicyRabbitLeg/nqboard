package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
