package com.mx.nqboard.device.api.constant;

/**
 * 告警
 *
 * @author SpicyRabbitLeg
 */
public interface IotAlarmRecordConstant {

    /**
     * 告警topic
     */
    String ALARM_RECORD_TOPIC = "alarm_record_topic";

    /**
     * 告警消费者
     */
    String ALARM_RECORD_CONSUMER = "alarm_record_consumer";

    /**
     * 告警唯一redis key
     */
    String ALARM_RECORD_KEY = "alarm_record_key:%s:%s";

	/**
	 * 告警设备在线类型
	 */
	String ALARM_VIDEO_TYPE_ONLINE = "STATUS";

	/**
	 * 告警设备识别类型
	 */
	String ALARM_VIDEO_TYPE_RECOGNIZE = "RECOGNIZE";
}
