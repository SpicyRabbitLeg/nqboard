package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockBacktestTradeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 回测成交明细 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockBacktestTradeMapper extends BaseMapper<StockBacktestTradeEntity> {

}
