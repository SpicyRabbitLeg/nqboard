package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.IndexDailyEntity;
import com.mx.nqboard.sniper.mapper.IndexDailyMapper;
import com.mx.nqboard.sniper.service.IndexDailyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 指数日行情 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/05
 */
@Slf4j
@Service
public class IndexDailyServiceImpl extends ServiceImpl<IndexDailyMapper, IndexDailyEntity> implements IndexDailyService {
}
