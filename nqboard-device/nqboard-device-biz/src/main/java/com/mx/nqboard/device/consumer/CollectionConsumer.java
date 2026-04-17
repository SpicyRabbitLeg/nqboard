package com.mx.nqboard.device.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.device.api.constant.IotAlarmRecordConstant;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.dto.IotDeviceFieldDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.IotPointService;
import com.mx.nqboard.device.service.IotProductService;
import com.mx.nqboard.device.service.impl.DriverCustomHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 主动采集设备数据入库
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotProductConstant.COLLECTION_TOPIC, consumerGroup = IotProductConstant.COLLECTION_GROUP)
public class CollectionConsumer implements RocketMQListener<String> {
    private final DriverCustomHandler customHandler;

    private final IotProductService productService;

    private final IotPointService pointService;

    private final RocketMQTemplate rocketMQTemplate;

    private final IotDeviceService deviceService;

    @Override
    public void onMessage(String message) {
        IotDeviceEntity entity = JSON.parseObject(message, IotDeviceEntity.class);
        log.info("设备采集开始 设备id:{}", entity.getId());

        IotProductEntity product = productService.getById(entity.getProductId());
        // 协议类型
        String driverType = product.getProtocol();
        // 驱动连接信息
        Map<String, Object> driverInfo = JSON.parseObject(entity.getDriverValue());
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

            try {
                // 端点连接信息
                Map<String, Object> pointInfo = pointVo.getPointInfo();
                pointInfo.put("dataType", model.getDataType());
                String value = customHandler.read(driverType, driverInfo, pointInfo);
                if(StrUtil.isNotBlank(value)) {
                    deviceService.setOnlineStatus(value, entity.getId());
                    dataValueList.add(new IotDeviceFieldDTO(entity.getId(), model.getIdentifier(), value, model.getDataType()));
                    log.info("读取到设备值：{}   设备:{}  端点:{}", value, entity.getName(), model.getName());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        // 设备入库
        if (CollUtil.isNotEmpty(dataValueList)) {
            rocketMQTemplate.convertAndSend(IotDeviceConstant.DEVICE_VALUE_TOPIC, JSON.toJSONString(dataValueList));
            rocketMQTemplate.convertAndSend(IotAlarmRecordConstant.ALARM_RECORD_TOPIC,JSON.toJSONString(dataValueList));
        }
    }
}
