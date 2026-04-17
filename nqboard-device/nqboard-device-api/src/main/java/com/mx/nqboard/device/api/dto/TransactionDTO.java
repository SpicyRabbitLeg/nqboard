package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分布式事务传输对象
 *
 * @author SpicyRabbitLeg
 **/
@Data
public class TransactionDTO<T> implements Serializable {

    /**
     * 业务类型（task_a、task_b）
     */
    @Schema(description = "业务类型（task_a、task_b）")
    private String businessType;

    /**
     * 业务数据
     */
    @Schema(description = "业务数据")
    private T bizObject;
}

