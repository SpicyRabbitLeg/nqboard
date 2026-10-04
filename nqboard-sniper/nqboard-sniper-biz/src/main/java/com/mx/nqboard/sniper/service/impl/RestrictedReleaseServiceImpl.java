package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity;
import com.mx.nqboard.sniper.mapper.RestrictedReleaseMapper;
import com.mx.nqboard.sniper.service.RestrictedReleaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 限售解禁 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class RestrictedReleaseServiceImpl extends ServiceImpl<RestrictedReleaseMapper, RestrictedReleaseEntity> implements RestrictedReleaseService {
}
