package com.mx.nqboard.export.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;
import com.mx.nqboard.export.mapper.ExtractRecordMapper;
import com.mx.nqboard.export.service.ExtractRecordService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 专家抽取记录 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Service
public class ExtractRecordServiceImpl extends ServiceImpl<ExtractRecordMapper, ExtractRecordEntity>
		implements ExtractRecordService {

	@Override
	public Page<ExtractRecordEntity> pageRecords(Page<ExtractRecordEntity> page, String keyword) {
		// 分页规范：先分页查 id（覆盖索引），再按 id in 查详情
		LambdaQueryWrapper<ExtractRecordEntity> idWrapper = Wrappers.<ExtractRecordEntity>lambdaQuery()
			.select(ExtractRecordEntity::getId)
			.like(StrUtil.isNotBlank(keyword), ExtractRecordEntity::getQueryText, keyword);
		if (CollUtil.isEmpty(page.orders())) {
			idWrapper.orderByDesc(ExtractRecordEntity::getId);
		}
		Page<ExtractRecordEntity> idPage = this.page(page, idWrapper);
		List<Long> ids = idPage.getRecords().stream().map(ExtractRecordEntity::getId).toList();
		if (CollUtil.isEmpty(ids)) {
			idPage.setRecords(Collections.emptyList());
			return idPage;
		}
		Map<Long, ExtractRecordEntity> detailMap = this.listByIds(ids).stream()
			.collect(Collectors.toMap(ExtractRecordEntity::getId, Function.identity()));
		idPage.setRecords(ids.stream().map(detailMap::get).filter(Objects::nonNull).toList());
		return idPage;
	}
}
