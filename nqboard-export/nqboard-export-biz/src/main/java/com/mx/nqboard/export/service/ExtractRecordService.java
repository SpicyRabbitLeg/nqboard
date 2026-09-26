package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.export.api.entity.ExtractRecordEntity;

/**
 * <p>
 * 专家抽取记录 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExtractRecordService extends IService<ExtractRecordEntity> {

	/**
	 * 抽取历史记录分页（先查 id 再查详情）
	 * @param page 分页对象
	 * @param keyword 查询内容关键词（可选）
	 * @return 分页结果
	 */
	Page<ExtractRecordEntity> pageRecords(Page<ExtractRecordEntity> page, String keyword);
}
