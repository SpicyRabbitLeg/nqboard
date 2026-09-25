package com.mx.nqboard.export.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.api.vo.ExpertExcelVO;
import org.springframework.validation.BindingResult;

import java.util.List;

/**
 * <p>
 * 专家管理 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public interface ExpertService extends IService<ExpertEntity> {

	/**
	 * 分页查询专家（先分页查 id，再按 id in 查详情，避免深分页回表）
	 * @param page 分页对象
	 * @param query 查询条件
	 * @return 分页结果
	 */
	Page<ExpertEntity> pageExperts(Page<ExpertEntity> page, ExpertEntity query);

	/**
	 * 学科门类下拉选项（库内去重）
	 * @return 门类列表
	 */
	List<String> listCategoryOptions();

	/**
	 * 一级学科下拉选项（库内去重，可按门类级联过滤）
	 * @param category 学科门类（可选）
	 * @return 一级学科列表
	 */
	List<String> listDisciplineOptions(String category);

	/**
	 * 导入专家信息
	 * @param excelVOList 专家Excel数据列表
	 * @param bindingResult 数据校验结果
	 * @return 导入结果
	 */
	R importExperts(List<ExpertExcelVO> excelVOList, BindingResult bindingResult);
}
