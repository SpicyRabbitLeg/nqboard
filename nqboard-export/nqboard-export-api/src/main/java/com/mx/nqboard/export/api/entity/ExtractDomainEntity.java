package com.mx.nqboard.export.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 专家抽取领域表
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@TableName("extract_domain")
@Schema(description = "专家抽取领域")
@EqualsAndHashCode(callSuper = true)
public class ExtractDomainEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 领域id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "领域id")
	private Long id;

	/**
	 * 领域编码
	 */
	@NotBlank(message = "领域编码 不能为空")
	@Schema(description = "领域编码")
	private String domainCode;

	/**
	 * 领域名称
	 */
	@NotBlank(message = "领域名称 不能为空")
	@Schema(description = "领域名称")
	private String domainName;

	/**
	 * 领域描述（AI判定依据）
	 */
	@Schema(description = "领域描述（AI判定依据）")
	private String description;

	/**
	 * 关键词（逗号分隔）
	 */
	@Schema(description = "关键词（逗号分隔）")
	private String keywords;

	/**
	 * 邻接领域编码JSON数组
	 */
	@Schema(description = "邻接领域编码JSON数组")
	private String adjacentCodes;

	/**
	 * 状态
	 */
	@Schema(description = "状态,0:启用,1:停用")
	private String status;

	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer seq;

	/**
	 * 删除标记
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;
}
