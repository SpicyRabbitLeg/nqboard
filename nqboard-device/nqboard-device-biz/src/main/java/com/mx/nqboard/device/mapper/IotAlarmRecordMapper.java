package com.mx.nqboard.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.device.api.entity.IotAlarmRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 告警记录管理 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface IotAlarmRecordMapper extends BaseMapper<IotAlarmRecordEntity> {
}
