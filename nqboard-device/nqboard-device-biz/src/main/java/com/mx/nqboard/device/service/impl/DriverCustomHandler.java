package com.mx.nqboard.device.service.impl;


import com.mx.nqboard.device.service.DriverCustomService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略模式
 *
 * @author 泥鳅压滑板
 */
@Log4j2
@Service
public class DriverCustomHandler {
    private final Map<String, DriverCustomService> handlers = new HashMap<>(6);

    /**
     * 注册链接协议接口
     *
     * @param driverType          driverType
     * @param driverCustomService driverCustomService
     */
    public void register(String driverType, DriverCustomService driverCustomService) {
        handlers.put(driverType, driverCustomService);
    }

    /**
     * 读取数据
     *
     * @param driverType 驱动类型
     * @param driverInfo 连接信息
     * @param pointInfo  端点信息
     * @return 获取到的实时值
     */
    public String read(String driverType, Map<String, Object> driverInfo, Map<String, Object> pointInfo) {
        DriverCustomService driverCustomService = handlers.get(driverType);
        return driverCustomService.read(driverInfo, pointInfo);
    }

    /**
     * 写入数据
     *
     * @param driverType 驱动类型
     * @param driverInfo 连接信息
     * @param pointInfo  端点信息
     * @return 写入是否成功
     */
    public Boolean write(String driverType, Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value) {
        DriverCustomService driverCustomService = handlers.get(driverType);
        return driverCustomService.write(driverInfo, pointInfo, value);
    }

}
