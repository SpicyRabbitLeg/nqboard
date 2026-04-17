package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.List;

/**
 * 摄像头AI服务注册请求DTO
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "摄像头AI服务注册请求")
public class VideoAnalysisDTO {

	/**
	 * RTSP流地址
	 */
    @Schema(description = "RTSP流地址")
    @NotBlank(message = "RTSP流地址不能为空")
    private String rtspUrl;

	/**
	 * RTSP用户名
	 */
	@Schema(description = "RTSP用户名")
    private String rtspUsername;

	/**
	 * RTSP密码
	 */
    @Schema(description = "RTSP密码")
    private String rtspPassword;

	/**
	 * 识别类型列表
	 */
	@Schema(description = "识别类型列表")
    @NotEmpty(message = "识别类型不能为空")
    private List<String> types;

	/**
	 * 设备ID
	 */
    @Schema(description = "设备ID")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

	/**
	 * 回调地址
	 */
	@Schema(description = "回调地址")
    @NotBlank(message = "回调地址不能为空")
    private String callbackUrl;

	/**
	 * 操作类型：pause-暂停，resume-恢复
	 */
	@Schema(description = "操作类型：pause-暂停，resume-恢复")
	@NotBlank(message = "操作类型不能为空")
	@Pattern(regexp = "pause|resume", message = "操作类型必须是pause或resume")
	private String operate;
}