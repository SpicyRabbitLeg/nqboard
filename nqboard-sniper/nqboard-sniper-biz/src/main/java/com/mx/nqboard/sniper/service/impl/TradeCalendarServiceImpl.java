package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.TradeCalendarEntity;
import com.mx.nqboard.sniper.mapper.TradeCalendarMapper;
import com.mx.nqboard.sniper.service.TradeCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 交易日历 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class TradeCalendarServiceImpl extends ServiceImpl<TradeCalendarMapper, TradeCalendarEntity> implements TradeCalendarService {
}
