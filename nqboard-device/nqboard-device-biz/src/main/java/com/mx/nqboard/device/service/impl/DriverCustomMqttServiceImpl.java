package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.common.core.constant.DriverConstants;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.exception.WritePointException;
import com.mx.nqboard.device.api.constant.MqttConstant;
import com.mx.nqboard.device.api.dto.MqttDeviceDTO;
import com.mx.nqboard.device.service.DriverCustomService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mqtt协议
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
public class DriverCustomMqttServiceImpl implements DriverCustomService {
    private final Map<String, MqttClient> clientMap = new ConcurrentHashMap<>(16);

    @Override
    public String read(Map<String, Object> driverInfo, Map<String, Object> pointInfo) {
        // mqtt通过监听获取数据，不用返回数据
        MqttClient client = getClient(driverInfo);
        return StringConstants.Other.EMPTY;
    }

    @Override
    public Boolean write(Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value) {
        return writeValue(getClient(driverInfo), pointInfo, value);
    }

    @Override
    public String getStatus() {
        return DriverConstants.MQTT;
    }

    /**
     * 获取mqtt客户端
     *
     * @param driverInfo 驱动信息
     * @return MqttClient
     */
    private synchronized MqttClient getClient(Map<String, Object> driverInfo) {
        log.debug("Mqtt client Server Connection Info {}", driverInfo);
        String serverUri = String.valueOf(driverInfo.get("serverUri"));
        String username = String.valueOf(driverInfo.get("username"));
        String password = String.valueOf(driverInfo.get("password"));
        int keepAliveInterval = Integer.parseInt(String.valueOf(driverInfo.getOrDefault("keepAliveInterval", "60")));
        int connectionTimeout = Integer.parseInt(String.valueOf(driverInfo.getOrDefault("connectionTimeout", "30")));

        MqttClient mqttClient = clientMap.get(serverUri);
        if (ObjectUtil.isNull(mqttClient)) {
            try {
                mqttClient = new MqttClient(serverUri, serverUri, new MemoryPersistence());

                // 设置连接选项
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                connOpts.setKeepAliveInterval(keepAliveInterval);
                connOpts.setConnectionTimeout(connectionTimeout);
                connOpts.setUserName(username);
                connOpts.setPassword(password.toCharArray());

                mqttClient.setCallback(new MqttCallbackImpl(serverUri));
                mqttClient.connect(connOpts);
                // 订阅主题
                mqttClient.subscribe(MqttConstant.MQTT_DEVICE_TOPIC, 2);
                clientMap.put(serverUri, mqttClient);
            } catch (Exception e) {
                log.error("get mqtt client error: {}", e.getMessage());
                clientMap.entrySet().removeIf(next -> next.getKey().equals(serverUri));
                throw new RuntimeException(e.getMessage());
            }
        }
        return mqttClient;
    }

    /**
     * 写入mqtt
     *
     * @param mqttClient client
     * @param pointInfo  位号信息
     * @param value      写入值
     */
    private boolean writeValue(MqttClient mqttClient, Map<String, Object> pointInfo, Object value) {
        try {
            log.info("pointInfo:{}  value:{}", pointInfo, value);
            String label = String.valueOf(pointInfo.get("label"));
            Long deviceId = Long.parseLong(String.valueOf(pointInfo.get("deviceId")));


            MqttDeviceDTO.Point point = new MqttDeviceDTO.Point();
            point.setLabel(label);
            point.setValue(value);

            MqttDeviceDTO mqttDeviceDto = new MqttDeviceDTO();
            mqttDeviceDto.setDeviceId(deviceId);
            mqttDeviceDto.setPointList(Collections.singletonList(point));

            MqttMessage message = new MqttMessage(JSON.toJSONString(mqttDeviceDto).getBytes(StandardCharsets.UTF_8));
            mqttClient.publish(MqttConstant.MQTT_DEVICE_WRITE,message);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("Write mqtt value error: {}", e.getMessage());
            throw new WritePointException(e.getMessage());
        }
    }

    /**
     * 移除
     *
     * @param clientId 客户端id
     */
    public void removeClient(String clientId) {
        clientMap.entrySet().removeIf(next -> next.getKey().equals(clientId));
    }
}
