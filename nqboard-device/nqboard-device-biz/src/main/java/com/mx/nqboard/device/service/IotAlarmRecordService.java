package com.mx.nqboard.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.device.api.dto.VideoAlarmRecordDTO;
import com.mx.nqboard.device.api.entity.IotAlarmRecordEntity;

/**
 * <p>
 * 告警记录管理 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface IotAlarmRecordService extends IService<IotAlarmRecordEntity> {
	/**
	 * 回调
	 * @param alarmRecordDto alarmRecordDto
	 * @return ok
	 */
	Boolean callback(VideoAlarmRecordDTO alarmRecordDto);
}
