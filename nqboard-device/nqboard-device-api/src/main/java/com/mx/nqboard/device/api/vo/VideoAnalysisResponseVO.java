package com.mx.nqboard.device.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 摄像头AI服务响应VO
 *
 * @author 泥鳅压滑板
 */
@Data
@Schema(description = "摄像头AI服务响应")
public class VideoAnalysisResponseVO {

    @Schema(description = "响应码")
    private Integer code;

    @Schema(description = "响应消息")
    private String msg;

    @Schema(description = "响应数据")
    private String data;
}