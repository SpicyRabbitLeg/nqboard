package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity;
import com.mx.nqboard.sniper.mapper.FinancialIndicatorMapper;
import com.mx.nqboard.sniper.service.FinancialIndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 财务指标（双腿） 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class FinancialIndicatorServiceImpl extends ServiceImpl<FinancialIndicatorMapper, FinancialIndicatorEntity> implements FinancialIndicatorService {
}
