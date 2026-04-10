package com.mx.nqboard.device.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 导入模型
 *
 * @author 泥鳅压滑板
 */
@Data
@Schema(description = "导入模型")
public class IotImportModelDTO {
    /**
     * 通用物模型ids
     */
    @Schema(description = "通用物模型ids")
    @NotNull(message = "通用物模型ids不能为空")
    private List<Long> templateIds;

    /**
     * 产品id
     */
    @Schema(description = "产品id")
    @NotNull(message = "产品id不能为空")
    private Long productId;
}
