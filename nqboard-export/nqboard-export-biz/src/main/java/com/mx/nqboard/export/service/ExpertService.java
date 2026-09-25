package com.mx.nqboard.export.service;

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
	 * 导入专家信息
	 * @param excelVOList 专家Excel数据列表
	 * @param bindingResult 数据校验结果
	 * @return 导入结果
	 */
	R importExperts(List<ExpertExcelVO> excelVOList, BindingResult bindingResult);
}
