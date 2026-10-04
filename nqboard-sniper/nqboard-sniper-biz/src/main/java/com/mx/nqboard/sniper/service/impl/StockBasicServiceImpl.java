package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.mapper.StockBasicMapper;
import com.mx.nqboard.sniper.service.StockBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 股票基础信息 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class StockBasicServiceImpl extends ServiceImpl<StockBasicMapper, StockBasicEntity> implements StockBasicService {
}
