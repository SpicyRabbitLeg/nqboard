package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.ScanRejectedEntity;
import com.mx.nqboard.sniper.mapper.ScanRejectedMapper;
import com.mx.nqboard.sniper.service.ScanRejectedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 扫描被拒明细 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class ScanRejectedServiceImpl extends ServiceImpl<ScanRejectedMapper, ScanRejectedEntity> implements ScanRejectedService {
}
