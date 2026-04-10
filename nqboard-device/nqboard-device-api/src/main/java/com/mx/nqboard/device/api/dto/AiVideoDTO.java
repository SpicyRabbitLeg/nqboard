package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author 泥鳅压滑板
 **/
@Data
public class AiVideoDTO {

    /**
     * 设备id
     */
    @Schema(description = "设备id")
    private Long deviceId;

    /**
     * 消息描述
     */
    @Schema(description = "消息描述")
    private String message;

    /**
     * base64图片
     */
    @Schema(description = "base64图片")
    private String imgStr;
}
