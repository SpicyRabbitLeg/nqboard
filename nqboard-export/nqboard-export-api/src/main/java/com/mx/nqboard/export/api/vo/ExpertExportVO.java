package com.mx.nqboard.export.api.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 专家导出excel
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@Schema(description = "专家导出excel")
public class ExpertExportVO {

	/**
	 * 专家id
	 */
	@Schema(description = "专家id")
	@ExcelProperty("专家id")
	private Long id;

	/**
	 * 学科门类
	 */
	@Schema(description = "学科门类")
	@ExcelProperty("学科门类")
	private String subjectCategory;

	/**
	 * 一级学科
	 */
	@Schema(description = "一级学科")
	@ExcelProperty("一级学科")
	private String firstDiscipline;

	/**
	 * 二级学科
	 */
	@Schema(description = "二级学科")
	@ExcelProperty("二级学科")
	private String secondDiscipline;

	/**
	 * 研究方向
	 */
	@Schema(description = "研究方向")
	@ExcelProperty("研究方向")
	private String researchDirection;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	@ExcelProperty("创建时间")
	private LocalDateTime createTime;
}
