package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author 泥鳅压滑板
 */
@Data
public class MqttDeviceDTO {
    /**
     * 设备id
     */
    @Schema(description = "设备id")
    private Long deviceId;

    /**
     * 点位列表
     */
    @Schema(description = "点位列表")
    private List<Point> pointList;


    @Data
    public static class Point {
        /**
         * 标签
         */
        @Schema(description = "标签")
        private String label;

        /**
         * 数据值
         */
        @Schema(description = "数据值")
        private Object value;
    }
}
