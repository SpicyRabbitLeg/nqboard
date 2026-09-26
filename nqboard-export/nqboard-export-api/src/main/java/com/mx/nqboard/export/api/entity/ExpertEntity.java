package com.mx.nqboard.export.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 专家表
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@TableName("expert_info")
@Schema(description = "专家")
@EqualsAndHashCode(callSuper = true)
public class ExpertEntity extends Model<ExpertEntity> {

	private static final long serialVersionUID = 1L;

	/**
	 * 专家id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "专家id")
	private Long id;

	/**
	 * 创建人
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 修改人
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 修改时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	/**
	 * 删除标记
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

	/**
	 * 专家名称
	 */
	@NotBlank(message = "专家名称 不能为空")
	@Schema(description = "专家名称")
	private String expertName;

	/**
	 * 学科门类
	 */
	@NotBlank(message = "学科门类 不能为空")
	@Schema(description = "学科门类")
	private String subjectCategory;

	/**
	 * 一级学科
	 */
	@NotBlank(message = "一级学科 不能为空")
	@Schema(description = "一级学科")
	private String firstDiscipline;

	/**
	 * 二级学科
	 */
	@Schema(description = "二级学科")
	private String secondDiscipline;

	/**
	 * 研究方向
	 */
	@Schema(description = "研究方向")
	private String researchDirection;

	/**
	 * 领域编码（AI打标）
	 */
	@Schema(description = "领域编码（AI打标）")
	private String domainCode;

	/**
	 * 领域名称
	 */
	@Schema(description = "领域名称")
	private String domainName;
}
