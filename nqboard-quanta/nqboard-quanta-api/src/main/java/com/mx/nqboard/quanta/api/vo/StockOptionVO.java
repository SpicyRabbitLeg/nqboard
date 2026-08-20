package com.mx.nqboard.quanta.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 股票下拉选项（前端下拉框专用，精简字段）
 *
 * @author SpicyRabbitLeg
 */
@Data
@Schema(description = "股票下拉选项")
public class StockOptionVO {

    /**
     * 股票代码 000001.SZ
     */
    @Schema(description = "股票代码 000001.SZ")
    private String tsCode;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    private String name;
}
