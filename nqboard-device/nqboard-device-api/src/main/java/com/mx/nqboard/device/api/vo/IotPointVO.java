package com.mx.nqboard.device.api.vo;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * @author SpicyRabbitLeg
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IotPointVO extends IotPointEntity {
    /**
     * 端点信息
     */
    @Schema(description = "端点信息")
    private Map<String,Object> pointInfo;

    /**
     * 模型信息
     */
    @Schema(description = "模型信息")
    private IotModelVO model;

    public Map<String,Object> getPointInfo() {
        String protocolValue = super.getProtocolValue();
        if(StrUtil.isNotBlank(protocolValue)) {
            return JSON.parseObject(protocolValue);
        }
        return null;
    }
}
