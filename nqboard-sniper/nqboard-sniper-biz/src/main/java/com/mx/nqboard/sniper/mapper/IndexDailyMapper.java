package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.IndexDailyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 指数日行情 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/05
 */
@Mapper
public interface IndexDailyMapper extends BaseMapper<IndexDailyEntity> {
}
