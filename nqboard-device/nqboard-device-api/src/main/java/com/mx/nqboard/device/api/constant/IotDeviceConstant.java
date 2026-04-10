package com.mx.nqboard.device.api.constant;

/**
 *
 *
 * @author 泥鳅压滑板
 */
public interface IotDeviceConstant {

    /**
     * 设备在线状态
     */
    String DEVICE_ONLINE_STATUS = "3";

    /**
     * 设备离线状态
     */
    String DEVICE_OFFLINE_STATUS = "4";

    /**
     * 唯一标识传入错误
     */
    String ERROR_IDENTIFIER = "iot.device.identifier";

    /**
     * 设备保存错误
     */
    String ERROR_DEVICE_SAVE = "iot.device.save";

    /**
     * 设备是否存在
     */
    String ERROR_DEVICE_EXIT = "iot.device.exit";

    /**
     * 设备删除失败
     */
    String ERROR_DEL = "iot.device.del";

    /**
     * 设备添加topic
     */
    String DEVICE_SAVE_TOPIC = "device_save_topic";

    /**
     * 设备删除topic
     */
    String DEVICE_DEL_TOPIC = "device_del_topic";

    /**
     * 设备值topic
     */
    String DEVICE_VALUE_TOPIC = "device_value_topic";

    /**
     * 设备值消费者
     */
    String DEVICE_VALUE_CONSUMER = "device_value_consumer";

    /**
     * 只读状态
     */
    String READ_ONLY_STATUS = "0";

    /**
     * 只写状态
     */
    String WRITE_ONLY_STATUS = "1";

    /**
     * 读写状态
     */
    String READ_WRITE_STATUS = "2";

    /**
     * 设备在线通配符replace
     */
    String DEVICE_ONLINE_ALL_REPLACE_KEY = "device:online:";

    /**
     * 设备在线key
     */
    String DEVICE_ONLINE_KEY = DEVICE_ONLINE_ALL_REPLACE_KEY + "%s";

    /**
     * 设备在线通配符
     */
    String DEVICE_ONLINE_ALL_KEY = DEVICE_ONLINE_ALL_REPLACE_KEY + "*";
}
