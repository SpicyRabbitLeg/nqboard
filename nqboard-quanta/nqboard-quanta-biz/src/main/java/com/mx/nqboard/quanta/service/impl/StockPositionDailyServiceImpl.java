package com.mx.nqboard.quanta.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockPositionDailyEntity;
import com.mx.nqboard.quanta.mapper.StockPositionDailyMapper;
import com.mx.nqboard.quanta.service.StockPositionDailyService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 模拟持仓逐日盯市 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Service
public class StockPositionDailyServiceImpl extends ServiceImpl<StockPositionDailyMapper, StockPositionDailyEntity>
		implements StockPositionDailyService {

}
