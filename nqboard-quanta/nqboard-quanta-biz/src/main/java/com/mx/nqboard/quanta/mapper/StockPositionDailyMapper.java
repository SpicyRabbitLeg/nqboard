package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockPositionDailyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 模拟持仓逐日盯市 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockPositionDailyMapper extends BaseMapper<StockPositionDailyEntity> {

}
