package com.mx.nqboard.device.api.constant;

/**
 * mqtt
 *
 * @author SpicyRabbitLeg
 */
public interface MqttConstant {
    /**
     * 设备topic
     */
    String MQTT_DEVICE_TOPIC = "$share/spicy/spicy-iot/device/+";

    /**
     * 写数据topic
     */
    String MQTT_DEVICE_WRITE = "spicy-iot/device/write";

    /**
     * 设备读数据topic
     */
    String MQTT_DEVICE_READ = "spicy-iot/device/read";
}
