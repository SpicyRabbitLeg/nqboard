package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 正常范围对象
 *
 * @author 泥鳅压滑板
 */
@Data
public class SpecsDTO {
    /**
     * 最大值
     */
    @Schema(description = "最大值")
    private Long max;

    /**
     * 最小值
     */
    @Schema(description = "最小值")
    private Long min;
}
