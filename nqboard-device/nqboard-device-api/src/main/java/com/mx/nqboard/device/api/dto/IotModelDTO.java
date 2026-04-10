package com.mx.nqboard.device.api.dto;

import com.mx.nqboard.device.api.entity.IotModelEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 物模型
 *
 * @author 泥鳅压滑板
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotModelDTO extends IotModelEntity {
    /**
     * 操作类型：add、edit、del
     */
    @Schema(description = "操作类型：add、edit、del")
    private String execType;

    /**
     * 幂等性uuid
     */
    @Schema(description = "幂等性uuid")
    private Long uuid;
}
