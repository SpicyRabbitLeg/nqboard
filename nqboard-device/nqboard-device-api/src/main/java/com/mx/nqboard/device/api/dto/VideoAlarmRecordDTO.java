package com.mx.nqboard.device.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摄像头AI服务注册请求DTO
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "视频告警回调")
public class VideoAlarmRecordDTO {
	/**
	 * 回调类型： RECOGNIZE（识别）、STATUS（在线状态）
	 */
	@Schema(description = "回调类型： RECOGNIZE（识别）、STATUS（在线状态）")
	private String callbackType;

	/**
	 * 设备ID
	 */
	@Schema(description = "设备ID")
	private String deviceId;

	/**
	 * RTSP流地址
	 */
	@Schema(description = "RTSP流地址")
	private String rtspUrl;

	/**
	 * 设备状态
	 */
	@Schema(description = "设备状态")
	private String deviceStatus;

	/**
	 * 分析状态
	 */
	@Schema(description = "分析状态")
	private String analysisStatus;

	/**
	 * 识别类型
	 */
	@Schema(description = "识别类型")
	private String recognizeType;

	/**
	 * 置信度
	 */
	@Schema(description = "置信度")
	private Double confidence;

	/**
	 * 帧时间
	 */
	@Schema(description = "帧时间")
	private LocalDateTime frameTime;

	/**
	 * 帧Base64编码
	 */
	@Schema(description = "帧Base64编码")
	private String frameBase64;

	/**
	 * 目标位置信息
	 */
	@Schema(description = "目标位置信息")
	private Object targetLocation;

}