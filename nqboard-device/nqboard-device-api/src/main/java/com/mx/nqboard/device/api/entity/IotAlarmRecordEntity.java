package com.mx.nqboard.device.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 告警记录表
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("iot_alarm_record")
@Schema(description = "产品分类")
@EqualsAndHashCode(callSuper = true)
public class IotAlarmRecordEntity extends Model<IotAlarmRecordEntity> {

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
     * 描述信息
     */
    @Schema(description = "描述信息")
    @NotEmpty(message = "描述信息不能为空")
    private String message;

    /**
     * 设备id
     */
    @Schema(description = "设备id")
    @NotNull(message = "设备id不能为空")
    private Long deviceId;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /**
     * 处理状态（0未处理、1处理、2不需要处理）
     */
    @Schema(description = "处理状态（0未处理、1处理、2不需要处理）")
    private String handleStatus;

    /**
     * base64图片
     */
    @Schema(description = "base64图片")
    private String imgStr;
}
