package com.mx.nqboard.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 流程监听器对象表
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("flw_listener")
@Schema(description = "流程监听器")
@EqualsAndHashCode(callSuper = true)
public class FlwListenerEntity extends Model<FlwListenerEntity> {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description="id")
    private Long id;

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
     * 排序字段
     */
    @Schema(description="排序字段")
    private Integer orderNum;

    /**
     * 删除状态（0未删除、1删除）
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description="删除状态（0未删除、1删除）")
    private String delFlag;

    /**
     * 监听器名称
     */
    @Schema(description = "监听器名称")
    @NotEmpty(message = "监听器名称不能为空")
    private String name;

    /**
     * 监听类型（1任务监听、2执行监听）
     */
    @Schema(description = "监听类型（1任务监听、2执行监听）")
    @NotEmpty(message = "监听类型不能为空")
    private String type;

    /**
     * 事件类型 任务监听：（create、assignment、complete、delete） 执行监听（start、end、take）
     */
    @Schema(description = "事件类型 任务监听：（create、assignment、complete、delete） 执行监听（start、end、take）")
    @NotEmpty(message = "事件类型不能为空")
    private String eventType;

    /**
     * 值类型（classListener：Java类、expressionListener：表达式、delegateExpressionListener代理表达式）
     */
    @Schema(description = "值类型（classListener：Java类、expressionListener：表达式、delegateExpressionListener代理表达式）")
    @NotEmpty(message = "值类型不能为空")
    private String valueType;

    /**
     * 执行内容
     */
    @Schema(description = "执行内容")
    @NotEmpty(message = "执行内容不能为空")
    private String value;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 描述信息
     */
    @Schema(description = "描述信息")
    private String remark;
}
