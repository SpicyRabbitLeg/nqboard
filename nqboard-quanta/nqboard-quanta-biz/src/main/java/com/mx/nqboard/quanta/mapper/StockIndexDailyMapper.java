package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 指数日线K线表 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockIndexDailyMapper extends BaseMapper<StockIndexDailyEntity> {

}
