package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.export.api.entity.ExtractRecordDetailEntity;

/**
 * <p>
 * 专家抽取记录明细 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExtractRecordDetailService extends IService<ExtractRecordDetailEntity> {

	/**
	 * 已选/候选明细分页（先查 id 再查详情）
	 * @param page 分页对象
	 * @param recordId 抽取记录id
	 * @param grade 档位（1已选、2候选）
	 * @return 分页结果
	 */
	Page<ExtractRecordDetailEntity> pageDetails(Page<ExtractRecordDetailEntity> page, Long recordId, String grade);
}
