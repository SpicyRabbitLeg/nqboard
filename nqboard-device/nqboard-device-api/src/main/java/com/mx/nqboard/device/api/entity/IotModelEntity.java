package com.mx.nqboard.device.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 物模型表
 * </p>
 * @author SpicyRabbitLeg
 */
@Data
@TableName("iot_model")
@Schema(description = "物模型")
@EqualsAndHashCode(callSuper = true)
public class IotModelEntity extends Model<IotModelEntity> {
    private static final long serialVersionUID = 1L;

    /**
     * 业务id
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "业务id")
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
    @TableField(fill = FieldFill.UPDATE)
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
    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    /**
     * 0-正常，1-删除
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "删除标记,1:已删除,0:正常")
    private String delFlag;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段")
    private Integer orderNum;

    /**
     * 产品id
     */
    @Schema(description = "产品id")
    @NotNull(message = "产品id不能为空")
    private Long productId;

    /**
     * 模型名称
     */
    @Schema(description = "模型名称")
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /**
     * 标识
     */
    @Schema(description = "标识")
    @NotBlank(message = "标识不能为空")
    private String identifier;

    /**
     * 类别（0读写、1只读、2只写）
     */
    @Schema(description = "类别（0读写、1只读、2只写）")
    @NotBlank(message = "类别不能为空")
    private String type;

    /**
     * 数据类型（integer、decimal、string、bool、array、enum）
     */
    @Schema(description = "数据类型（integer、decimal、string、bool、array、enum）")
    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    /**
     * 基础值
     */
    @Schema(description = "基础值")
    private String baseValue;

    /**
     * 比例系数
     */
    @Schema(description = "比例系数")
    private Integer proportionalCoefficient;

    /**
     * 描述信息
     */
    @Schema(description = "描述信息")
    private String remark;

    /**
     * 正常值范围{min:0,max:100}
     */
    @Schema(description = "正常值范围{min:0,max:100}")
    private String specs;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;
}
