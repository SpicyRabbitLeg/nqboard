package com.mx.nqboard.export.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * <p>
 * 专家抽取记录明细表（存已选/候选/复核剔除，未匹配按记录动态计算）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@TableName("extract_record_detail")
@Schema(description = "专家抽取记录明细")
@EqualsAndHashCode(callSuper = true)
public class ExtractRecordDetailEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 明细id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "明细id")
	private Long id;

	/**
	 * 抽取记录id
	 */
	@Schema(description = "抽取记录id")
	private Long recordId;

	/**
	 * 专家id
	 */
	@Schema(description = "专家id")
	private Long expertId;

	/**
	 * 专家名称（冗余）
	 */
	@Schema(description = "专家名称（冗余）")
	private String expertName;

	/**
	 * 学科门类（冗余）
	 */
	@Schema(description = "学科门类（冗余）")
	private String subjectCategory;

	/**
	 * 一级学科（冗余）
	 */
	@Schema(description = "一级学科（冗余）")
	private String firstDiscipline;

	/**
	 * 档位
	 */
	@Schema(description = "档位,1:已选,2:候选,3:复核剔除")
	private String grade;

	/**
	 * 匹配得分
	 */
	@Schema(description = "匹配得分")
	private BigDecimal score;

	/**
	 * 命中理由
	 */
	@Schema(description = "命中理由")
	private String reason;

	/**
	 * 研究方向（关联专家表动态填充，非本表字段）
	 */
	@TableField(exist = false)
	@Schema(description = "研究方向")
	private String researchDirection;

	/**
	 * 删除标记
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;
}
