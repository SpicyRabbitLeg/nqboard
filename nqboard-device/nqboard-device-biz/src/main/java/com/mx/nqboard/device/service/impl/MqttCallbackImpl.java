package com.mx.nqboard.device.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotAlarmRecordConstant;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.constant.MqttConstant;
import com.mx.nqboard.device.api.dto.IotDeviceFieldDTO;
import com.mx.nqboard.device.api.dto.MqttDeviceDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.IotPointService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * mqtt callback
 *
 * @author 泥鳅压滑板
 */
@Slf4j
public class MqttCallbackImpl implements MqttCallback {

    private final String clientId;

    private final IotDeviceService deviceService;

    private final IotPointService pointService;

    private final RocketMQTemplate rocketMQTemplate;

    public MqttCallbackImpl(String clientId) {
        this.clientId = clientId;
        this.deviceService = SpringUtil.getBean(IotDeviceService.class);
        this.pointService = SpringUtil.getBean(IotPointService.class);
        this.rocketMQTemplate = SpringUtil.getBean(RocketMQTemplate.class);
    }


    @Override
    public void connectionLost(Throwable throwable) {
        SpringUtil.getBean(DriverCustomMqttServiceImpl.class).removeClient(this.clientId);
        log.info("connection lost error: {}", throwable.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String message = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        log.info("消费一条消息 topic:{}  message: {}", topic, message);

        // 设备数据入库
        if (MqttConstant.MQTT_DEVICE_READ.equals(topic)) {
            MqttDeviceDTO mqttDeviceDto = JSON.parseObject(message, MqttDeviceDTO.class);
            IotDeviceEntity entity = deviceService.getById(mqttDeviceDto.getDeviceId());

            // 设备数据记录
            List<IotDeviceFieldDTO> dataValueList = new LinkedList<>();

            IotPointEntity pointEntity = new IotPointEntity();
            pointEntity.setDeviceId(entity.getId());
            for (IotPointVO pointVo : pointService.getPointDetailByDeviceId(pointEntity)) {
                IotModelVO model = pointVo.getModel();
                // 只写类型则不进行操作
                if (IotDeviceConstant.WRITE_ONLY_STATUS.equals(model.getType())) {
                    continue;
                }

                // mqtt中的属性匹配
                for (MqttDeviceDTO.Point point : mqttDeviceDto.getPointList()) {
                    if (ObjectUtil.equals(point.getLabel(), pointVo.getPointInfo().get("label"))) {
                        deviceService.setOnlineStatus(String.valueOf(point.getValue()), entity.getId());
                        dataValueList.add(new IotDeviceFieldDTO(entity.getId(), model.getIdentifier(), point.getValue(), model.getDataType()));
                        log.info("读取到设备值：{}   设备:{}  端点:{}", point.getValue(), entity.getName(), model.getName());
                    }
                }
            }

            // 设备入库
            if (CollUtil.isNotEmpty(dataValueList)) {
                rocketMQTemplate.convertAndSend(IotDeviceConstant.DEVICE_VALUE_TOPIC, JSON.toJSONString(dataValueList));
                rocketMQTemplate.convertAndSend(IotAlarmRecordConstant.ALARM_RECORD_TOPIC, JSON.toJSONString(dataValueList));
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.info("deliveryComplete  token:{}", iMqttDeliveryToken);
    }
}
