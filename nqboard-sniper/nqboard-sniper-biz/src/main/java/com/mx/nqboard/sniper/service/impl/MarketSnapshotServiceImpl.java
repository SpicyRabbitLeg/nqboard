package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import com.mx.nqboard.sniper.mapper.MarketSnapshotMapper;
import com.mx.nqboard.sniper.service.MarketSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 全市场快照 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class MarketSnapshotServiceImpl extends ServiceImpl<MarketSnapshotMapper, MarketSnapshotEntity> implements MarketSnapshotService {
}
