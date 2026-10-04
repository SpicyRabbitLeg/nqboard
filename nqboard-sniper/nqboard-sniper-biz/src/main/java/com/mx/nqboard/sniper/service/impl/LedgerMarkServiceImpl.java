package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.LedgerMarkEntity;
import com.mx.nqboard.sniper.mapper.LedgerMarkMapper;
import com.mx.nqboard.sniper.service.LedgerMarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 台账每日mark 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class LedgerMarkServiceImpl extends ServiceImpl<LedgerMarkMapper, LedgerMarkEntity> implements LedgerMarkService {
}
