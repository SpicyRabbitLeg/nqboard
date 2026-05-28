package com.mx.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程表达式
 *
 * @author spicy
 * @date 2025-10-23 13:50:06
 */
@Data
@TableName("flw_expression")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程表达式")
public class FlwExpressionEntity extends Model<FlwExpressionEntity> {
	/**
	* id
	*/
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description="id")
    private Long id;

	/**
	* 排序字段
	*/
    @Schema(description="排序字段")
    private Integer orderNum;

	/**
	* 创建人
	*/
	@TableField(fill = FieldFill.INSERT)
    @Schema(description="创建人")
    private String createBy;

	/**
	* 创建时间
	*/
	@TableField(fill = FieldFill.INSERT)
    @Schema(description="创建时间")
    private LocalDateTime createTime;

	/**
	* 修改人
	*/
	@TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description="修改人")
    private String updateBy;

	/**
	* 修改时间
	*/
	@TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description="修改时间")
    private LocalDateTime updateTime;

	/**
	* 删除状态（0未删除、1删除）
	*/
    @TableLogic
	@TableField(fill = FieldFill.INSERT)
    @Schema(description="删除状态（0未删除、1删除）")
    private String delFlag;

	/**
	 * 名称
	 */
	@Schema(description="名称")
	private String name;

	/**
	 * 表达式内容
	 */
	@Schema(description="表达式内容")
	private String expression;

	/**
	 * 表达式类型
	 */
	@Schema(description="表达式类型")
	private String dataType;

	/**
	 * 状态（0成功、1失败）
	 */
	@Schema(description="状态（0成功、1失败）")
	private String status;

	/**
	 * 描述信息
	 */
	@Schema(description="描述信息")
	private String remark;
}
