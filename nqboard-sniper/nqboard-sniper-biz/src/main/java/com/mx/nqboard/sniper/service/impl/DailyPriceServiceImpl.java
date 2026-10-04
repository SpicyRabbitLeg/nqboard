package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.mapper.DailyPriceMapper;
import com.mx.nqboard.sniper.service.DailyPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 日行情 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class DailyPriceServiceImpl extends ServiceImpl<DailyPriceMapper, DailyPriceEntity> implements DailyPriceService {
}
