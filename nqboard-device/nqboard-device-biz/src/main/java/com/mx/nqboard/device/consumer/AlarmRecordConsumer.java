package com.mx.nqboard.device.consumer;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.RedisUtils;
import com.mx.nqboard.device.api.constant.IotAlarmRecordConstant;
import com.mx.nqboard.device.api.dto.IotDeviceFieldDTO;
import com.mx.nqboard.device.api.dto.SpecsDTO;
import com.mx.nqboard.device.api.entity.IotAlarmRecordEntity;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.service.IotAlarmRecordService;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.IotPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 设备告警-生成告警记录
 *
 * @author 泥鳅压滑板
 **/
@Log4j2
@Component
@Order
@RequiredArgsConstructor
@RocketMQMessageListener(topic = IotAlarmRecordConstant.ALARM_RECORD_TOPIC, consumerGroup = IotAlarmRecordConstant.ALARM_RECORD_CONSUMER)
public class AlarmRecordConsumer implements RocketMQListener<String> {

    private final IotAlarmRecordService alarmRecordService;

    private final IotPointService pointService;

    private final IotDeviceService deviceService;

    @Override
    public void onMessage(String message) {
        List<IotDeviceFieldDTO> iotDeviceFieldDtoList = JSON.parseArray(message, IotDeviceFieldDTO.class);
        for (IotDeviceFieldDTO deviceFieldDto : iotDeviceFieldDtoList) {
            IotDeviceEntity deviceEntity = deviceService.getById(deviceFieldDto.getDeviceId());

            IotPointEntity pointEntity = new IotPointEntity();
            pointEntity.setDeviceId(deviceEntity.getId());
            for (IotPointVO pointVo : pointService.getPointDetailByDeviceId(pointEntity)) {
                IotModelVO model = pointVo.getModel();
                if (ObjectUtil.isNull(model)  || StrUtil.isBlank(model.getSpecs()) || !ObjectUtil.equal(deviceFieldDto.getField(), model.getIdentifier())) {
                    continue;
                }

                // 是否具备阈值选项
                double value = Double.parseDouble(String.valueOf(deviceFieldDto.getValue()));
                SpecsDTO specsDto = JSON.parseObject(model.getSpecs(), SpecsDTO.class);

                String msg = "设备数据异常，设备%s 设备名称：%s  设备端点：%s  设备值：%s";
                if (specsDto.getMax() != 0 && specsDto.getMax() <= value) {
                    saveAlarmRecord(String.format(msg, "超于阈值", deviceEntity.getName(), model.getName(), value), deviceEntity.getId(), pointVo.getId(),deviceEntity.getUserId());
                } else if (specsDto.getMax() != 0 && specsDto.getMax() * 0.8 <= value) {
                    saveAlarmRecord(String.format(msg, "阈值上限(80%)", deviceEntity.getName(), model.getName(), value), deviceEntity.getId(), pointVo.getId(),deviceEntity.getUserId());
                } else if (specsDto.getMin() != 0 && specsDto.getMin() >= value) {
                    saveAlarmRecord(String.format(msg, "低于阈值", deviceEntity.getName(), model.getName(), value), deviceEntity.getId(), pointVo.getId(),deviceEntity.getUserId());
                }
            }
        }
    }

    /**
     * 添加告警数据
     *
     * @param message  message
     * @param deviceId deviceId
     */
    private void saveAlarmRecord(String message, Long deviceId, Long pointId,Long userId) {
        final String key = String.format(IotAlarmRecordConstant.ALARM_RECORD_KEY, deviceId, pointId);
        String flag = RedisUtils.get(key);
        if (StrUtil.isNotBlank(flag)) {
            return;
        }

        IotAlarmRecordEntity alarmRecordEntity = new IotAlarmRecordEntity();
        alarmRecordEntity.setMessage(message);
        alarmRecordEntity.setHandleStatus(StringConstants.Switch.DISABLE);
        alarmRecordEntity.setDeviceId(deviceId);
        alarmRecordEntity.setUserId(Optional.ofNullable(userId).orElse(1L));
        alarmRecordService.save(alarmRecordEntity);
        RedisUtils.set(key, "temp", 5, TimeUnit.HOURS);
    }
}
