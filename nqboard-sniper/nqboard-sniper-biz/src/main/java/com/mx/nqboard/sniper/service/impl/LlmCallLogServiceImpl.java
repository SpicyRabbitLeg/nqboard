package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.LlmCallLogEntity;
import com.mx.nqboard.sniper.mapper.LlmCallLogMapper;
import com.mx.nqboard.sniper.service.LlmCallLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * LLM调用审计 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class LlmCallLogServiceImpl extends ServiceImpl<LlmCallLogMapper, LlmCallLogEntity> implements LlmCallLogService {
}
