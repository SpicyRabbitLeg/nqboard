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
 * 设备端点表
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("iot_point")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备端点")
public class IotPointEntity extends Model<IotPointEntity> {
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
	* 协议连接信息具体值json
	*/
    @Schema(description="协议连接信息具体值json")
	@NotEmpty(message = "协议连接信息具体值jsonid不能为空")
    private String protocolValue;

	/**
	* 设备id
	*/
    @Schema(description="设备id")
	@NotNull(message = "设备id不能为空")
    private Long deviceId;

	/**
	* 物模型id
	*/
    @Schema(description="物模型id")
	@NotNull(message = "物模型id不能为空")
    private Long modelId;
}
