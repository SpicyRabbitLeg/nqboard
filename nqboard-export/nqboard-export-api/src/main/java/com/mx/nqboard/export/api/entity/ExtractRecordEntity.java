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
 * 专家抽取记录表（每次抽取一条；未匹配不入库，按记录动态计算）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Data
@TableName("extract_record")
@Schema(description = "专家抽取记录")
@EqualsAndHashCode(callSuper = true)
public class ExtractRecordEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 记录id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "记录id")
	private Long id;

	/**
	 * 用户自然语言查询
	 */
	@NotBlank(message = "查询内容 不能为空")
	@Schema(description = "用户自然语言查询")
	private String queryText;

	/**
	 * 解析出的领域编码JSON数组
	 */
	@Schema(description = "解析出的领域编码JSON数组")
	private String parsedDomains;

	/**
	 * 解析出的关键词JSON数组
	 */
	@Schema(description = "解析出的关键词JSON数组")
	private String parsedKeywords;

	/**
	 * 已选数量
	 */
	@Schema(description = "已选数量")
	private Integer selectedCount;

	/**
	 * 候选数量
	 */
	@Schema(description = "候选数量")
	private Integer candidateCount;

	/**
	 * 未匹配数量（抽取时点快照）
	 */
	@Schema(description = "未匹配数量（抽取时点快照）")
	private Integer unmatchedCount;

	/**
	 * 耗时（毫秒）
	 */
	@Schema(description = "耗时（毫秒）")
	private Integer costMs;

	/**
	 * 状态
	 */
	@Schema(description = "状态,0:成功,1:失败")
	private String status;

	/**
	 * 失败原因
	 */
	@Schema(description = "失败原因")
	private String failReason;

	/**
	 * 删除标记
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;
}
