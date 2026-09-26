package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.common.core.exception.CheckedException;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.entity.ExtractRecordDetailEntity;
import com.mx.nqboard.export.mapper.ExtractRecordDetailMapper;
import com.mx.nqboard.export.service.ExpertService;
import com.mx.nqboard.export.service.ExtractRecordDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家抽取记录明细 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Service
@RequiredArgsConstructor
public class ExtractRecordDetailServiceImpl extends ServiceImpl<ExtractRecordDetailMapper, ExtractRecordDetailEntity>
		implements ExtractRecordDetailService {

	/**
	 * 档位：1已选、2候选
	 */
	private static final String GRADE_SELECTED = "1";

	private static final String GRADE_CANDIDATE = "2";

	private final ExpertService expertService;

	@Override
	public Page<ExtractRecordDetailEntity> pageDetails(Page<ExtractRecordDetailEntity> page, Long recordId, String grade) {
		if (!GRADE_SELECTED.equals(grade) && !GRADE_CANDIDATE.equals(grade)) {
			throw new CheckedException("非法档位: " + grade);
		}
		// 分页规范：先分页查 id（命中 idx_record_grade 覆盖索引），再按 id in 查详情
		LambdaQueryWrapper<ExtractRecordDetailEntity> idWrapper = Wrappers.<ExtractRecordDetailEntity>lambdaQuery()
			.select(ExtractRecordDetailEntity::getId)
			.eq(ExtractRecordDetailEntity::getRecordId, recordId)
			.eq(ExtractRecordDetailEntity::getGrade, grade);
		if (CollUtil.isEmpty(page.orders())) {
			idWrapper.orderByDesc(ExtractRecordDetailEntity::getScore).orderByDesc(ExtractRecordDetailEntity::getId);
		}
		Page<ExtractRecordDetailEntity> idPage = this.page(page, idWrapper);
		List<Long> ids = idPage.getRecords().stream().map(ExtractRecordDetailEntity::getId).toList();
		if (CollUtil.isEmpty(ids)) {
			idPage.setRecords(Collections.emptyList());
			return idPage;
		}
		Map<Long, ExtractRecordDetailEntity> detailMap = this.listByIds(ids).stream()
			.collect(Collectors.toMap(ExtractRecordDetailEntity::getId, Function.identity()));
		List<ExtractRecordDetailEntity> records = ids.stream().map(detailMap::get).filter(Objects::nonNull).toList();
		fillResearchDirection(records);
		idPage.setRecords(records);
		return idPage;
	}

	/**
	 * 按本页 expertId 批量回填研究方向（明细表不冗余该字段，关联专家表动态填充）
	 * @param records 本页明细记录
	 */
	private void fillResearchDirection(List<ExtractRecordDetailEntity> records) {
		List<Long> expertIds = records.stream().map(ExtractRecordDetailEntity::getExpertId)
			.filter(Objects::nonNull).distinct().toList();
		if (CollUtil.isEmpty(expertIds)) {
			return;
		}
		Map<Long, String> directionMap = expertService.listByIds(expertIds).stream()
			.filter(e -> e.getResearchDirection() != null)
			.collect(Collectors.toMap(ExpertEntity::getId, ExpertEntity::getResearchDirection));
		records.forEach(r -> r.setResearchDirection(directionMap.get(r.getExpertId())));
	}

}
