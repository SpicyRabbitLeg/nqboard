package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.QuantSyncLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 数据同步执行日志 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface QuantSyncLogMapper extends BaseMapper<QuantSyncLogEntity> {

}
