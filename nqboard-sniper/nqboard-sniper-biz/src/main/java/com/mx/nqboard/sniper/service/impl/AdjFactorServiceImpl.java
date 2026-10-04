package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.mapper.AdjFactorMapper;
import com.mx.nqboard.sniper.service.AdjFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 复权因子 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class AdjFactorServiceImpl extends ServiceImpl<AdjFactorMapper, AdjFactorEntity> implements AdjFactorService {
}
