package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.DragonTigerEntity;
import com.mx.nqboard.sniper.mapper.DragonTigerMapper;
import com.mx.nqboard.sniper.service.DragonTigerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 龙虎榜 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class DragonTigerServiceImpl extends ServiceImpl<DragonTigerMapper, DragonTigerEntity> implements DragonTigerService {
}
