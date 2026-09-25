package com.mx.nqboard.export.api.vo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import com.pig4cloud.plugin.excel.annotation.ExcelLine;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 专家信息 Excel 导入对象（按表头名称映射列）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@ColumnWidth(30)
public class ExpertExcelVO implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 行号
	 */
	@ExcelLine
	@ExcelIgnore
	private Long lineNum;

	/**
	 * 专家名称
	 */
	@NotBlank(message = "专家名称不能为空")
	@ExcelProperty("专家名称")
	private String expertName;

	/**
	 * 学科门类
	 */
	@NotBlank(message = "学科门类不能为空")
	@ExcelProperty("专家学科")
	private String subjectCategory;

	/**
	 * 一级学科
	 */
	@NotBlank(message = "一级学科不能为空")
	@ExcelProperty("专家一级学科")
	private String firstDiscipline;

	/**
	 * 研究方向
	 */
	@NotBlank(message = "研究方向不能为空")
	@ExcelProperty("专家研究方向")
	private String researchDirection;
}
