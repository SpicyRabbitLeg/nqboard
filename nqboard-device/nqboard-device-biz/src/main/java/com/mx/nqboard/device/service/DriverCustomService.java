package com.mx.nqboard.device.service;

import com.mx.nqboard.device.service.impl.DriverCustomHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 驱动自定义
 *
 * @author SpicyRabbitLeg
 */
public interface DriverCustomService {
    /**
     * 读取数据
     *
     * @param driverInfo driverInfo
     * @param pointInfo  pointInfo
     * @return 获取到的实时值
     */
    String read(Map<String, Object> driverInfo, Map<String, Object> pointInfo);

    /**
     * 写入数据
     *
     * @param driverInfo driverInfo
     * @param pointInfo  pointInfo
     * @param value value
     * @return 写入是否成功
     */
    Boolean write(Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value);

    /**
     * 获取驱动类型
     *
     * @return 驱动类型
     */
    String getStatus();

    /**
     * 将实现类注册到driverHandler中
     *
     * @param driverHandler driverHandler
     */
    @Autowired
    default void selfRegistration(DriverCustomHandler driverHandler) {
        driverHandler.register(getStatus(), this);
    }
}
