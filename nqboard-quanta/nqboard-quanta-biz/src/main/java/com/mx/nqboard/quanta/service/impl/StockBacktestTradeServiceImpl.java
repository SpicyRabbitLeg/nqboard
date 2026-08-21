package com.mx.nqboard.quanta.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBacktestTradeEntity;
import com.mx.nqboard.quanta.mapper.StockBacktestTradeMapper;
import com.mx.nqboard.quanta.service.StockBacktestTradeService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 回测成交明细 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Service
public class StockBacktestTradeServiceImpl extends ServiceImpl<StockBacktestTradeMapper, StockBacktestTradeEntity>
		implements StockBacktestTradeService {

}
