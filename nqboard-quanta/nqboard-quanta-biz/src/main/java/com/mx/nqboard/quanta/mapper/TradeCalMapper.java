package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.TradeCalEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 交易日历（tushare trade_cal） Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface TradeCalMapper extends BaseMapper<TradeCalEntity> {

}
