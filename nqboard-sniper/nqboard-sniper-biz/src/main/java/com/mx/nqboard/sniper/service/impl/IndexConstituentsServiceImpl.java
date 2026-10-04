package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity;
import com.mx.nqboard.sniper.mapper.IndexConstituentsMapper;
import com.mx.nqboard.sniper.service.IndexConstituentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 指数成分快照 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class IndexConstituentsServiceImpl extends ServiceImpl<IndexConstituentsMapper, IndexConstituentsEntity> implements IndexConstituentsService {
}
