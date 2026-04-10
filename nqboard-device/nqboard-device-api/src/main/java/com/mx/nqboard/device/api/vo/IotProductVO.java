package com.mx.nqboard.device.api.vo;

import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.entity.IotCategoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * 产品
 *
 * @author 泥鳅压滑板
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotProductVO extends IotProductEntity implements Serializable {
    /**
     * 产品分类对象
     */
    @Schema(description = "产品分类对象")
    private IotCategoryEntity iotCategory;
}
