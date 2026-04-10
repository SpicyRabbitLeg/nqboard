package com.mx.nqboard.device.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备表
 *
 * @author 泥鳅压滑板
 */
@Data
@TableName("iot_device")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "设备表")
public class IotDeviceEntity extends Model<IotDeviceEntity> {
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
	* 设备名称
	*/
    @Schema(description="设备名称")
	@NotBlank(message = "设备名称不能为空")
    private String name;

	/**
	 * 唯一标识
	 */
	@Schema(description = "唯一标识")
    private String identifier;

	/**
	* 设备经度
	*/
    @Schema(description="设备经度")
    private BigDecimal longitude;

	/**
	* 设备纬度
	*/
    @Schema(description="设备纬度")
    private BigDecimal latitude;

	/**
	 * 地理位置
	 */
	@Schema(description = "地理位置")
	@NotNull(message = "地理位置不能为空")
    private String location;

	/**
	* 设备状态（0关闭、1开启、2禁用）
	*/
    @Schema(description="设备状态（0关闭、1开启、2禁用）")
	@NotBlank(message = "设备状态不能为空")
    private String status;

	/**
	* 备注
	*/
    @Schema(description="备注")
    private String remark;

	/**
	* 产品id
	*/
    @Schema(description="产品id")
	@NotNull(message = "产品id不能为空")
    private Long productId;

	/**
	 * 驱动连接配置
	 */
	@Schema(description = "驱动连接配置")
    private String driverValue;

	/**
	* 定时任务cron表达式
	*/
    @Schema(description="定时任务cron表达式")
    private String cron;

	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
    private Long userId;
}
