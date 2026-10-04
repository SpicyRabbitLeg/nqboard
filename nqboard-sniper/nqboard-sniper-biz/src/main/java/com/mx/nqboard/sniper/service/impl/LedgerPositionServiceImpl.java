package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.LedgerPositionEntity;
import com.mx.nqboard.sniper.mapper.LedgerPositionMapper;
import com.mx.nqboard.sniper.service.LedgerPositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 模拟台账仓位 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class LedgerPositionServiceImpl extends ServiceImpl<LedgerPositionMapper, LedgerPositionEntity> implements LedgerPositionService {
}
