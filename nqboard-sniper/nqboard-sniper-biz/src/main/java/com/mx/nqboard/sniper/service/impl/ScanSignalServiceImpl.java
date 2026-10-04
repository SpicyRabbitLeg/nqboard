package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.ScanSignalEntity;
import com.mx.nqboard.sniper.mapper.ScanSignalMapper;
import com.mx.nqboard.sniper.service.ScanSignalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 扫描信号明细 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class ScanSignalServiceImpl extends ServiceImpl<ScanSignalMapper, ScanSignalEntity> implements ScanSignalService {
}
