package com.mx.nqboard.device.api.dto;

import com.mx.nqboard.device.api.entity.IotProductEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author 泥鳅压滑板
 **/
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotProductDTO extends IotProductEntity {
    /**
     * 幂等性uuid
     */
    @Schema(description = "幂等性uuid")
    private Long uuid;
}
