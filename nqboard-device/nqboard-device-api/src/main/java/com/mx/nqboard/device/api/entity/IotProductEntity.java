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
 * 产品表
 *
 * @author 泥鳅压滑板
 */
@Data
@TableName("iot_product")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "产品表")
public class IotProductEntity extends Model<IotProductEntity> {
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
     * 删除状态（0未删除、1删除）
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description="删除状态（0未删除、1删除）")
    private String delFlag;

    /**
     * 排序字段
     */
    @Schema(description="排序字段")
    private Integer orderNum;

    /**
     * 分类id
     */
    @Schema(description="分类id")
    @NotNull(message = "请传入产品分类")
    private Long categoryId;

    /**
     * 产品名称
     */
    @Schema(description="产品名称")
    @NotBlank(message = "请传入产品名称")
    private String name;

    /**
     * 产品标识
     */
    @Schema(description="产品标识")
    @NotBlank(message = "请传入产品标识")
    private String identifier;

    /**
     * 备注
     */
    @Schema(description="备注")
    private String remark;

    /**
     * 连接协议
     */
    @Schema(description="连接协议")
    @NotBlank(message = "请传入连接协议")
    private String protocol;

    /**
     * 授权开关
     */
    @Schema(description="授权开关")
    private Boolean authSwitch;

    /**
     * 授权模式（0简单、1加密）
     */
    @Schema(description="授权模式（0简单、1加密）")
    private String authMode;

    /**
     * 图标
     */
    @Schema(description="图标")
    private String img;

    /**
     * 镜头类型
     */
    @Schema(description = "镜头类型")
    private String cameraTypes;
}
