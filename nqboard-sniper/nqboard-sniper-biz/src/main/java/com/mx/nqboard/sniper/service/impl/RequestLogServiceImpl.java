package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.RequestLogEntity;
import com.mx.nqboard.sniper.mapper.RequestLogMapper;
import com.mx.nqboard.sniper.service.RequestLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 外部请求审计 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class RequestLogServiceImpl extends ServiceImpl<RequestLogMapper, RequestLogEntity> implements RequestLogService {
}
