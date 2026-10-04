package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.ScanReportEntity;
import com.mx.nqboard.sniper.mapper.ScanReportMapper;
import com.mx.nqboard.sniper.service.ScanReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 扫描报告头 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class ScanReportServiceImpl extends ServiceImpl<ScanReportMapper, ScanReportEntity> implements ScanReportService {
}
