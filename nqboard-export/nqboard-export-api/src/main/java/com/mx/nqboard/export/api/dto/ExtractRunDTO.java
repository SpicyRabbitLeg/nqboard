package com.mx.nqboard.export.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 专家抽取请求 DTO
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@Schema(description = "专家抽取请求")
public class ExtractRunDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 自然语言抽取条件
	 */
	@NotBlank(message = "抽取条件 不能为空")
	@Schema(description = "自然语言抽取条件，如：找人工智能、机器学习等相关专业专家")
	private String query;
}
