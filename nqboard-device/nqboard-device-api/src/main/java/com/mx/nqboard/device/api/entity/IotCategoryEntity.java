package com.mx.nqboard.device.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 产品分类表
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Data
@TableName("iot_category")
@Schema(description = "产品分类")
@EqualsAndHashCode(callSuper = true)
public class IotCategoryEntity extends Model<IotCategoryEntity> {

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
     * 产品名称
     */
    @NotBlank(message = "产品名称 不能为空")
    @Schema(description = "产品名称")
    private String name;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
