package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.RequestLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 外部请求审计 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLogEntity> {
}
