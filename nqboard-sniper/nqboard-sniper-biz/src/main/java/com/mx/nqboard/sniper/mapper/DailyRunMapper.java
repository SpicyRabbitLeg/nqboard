package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 每日运行记账 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface DailyRunMapper extends BaseMapper<DailyRunEntity> {
}
