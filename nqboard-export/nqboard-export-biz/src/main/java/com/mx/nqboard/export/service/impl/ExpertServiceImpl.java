package com.mx.nqboard.export.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.export.api.entity.ExpertEntity;
import com.mx.nqboard.export.mapper.ExpertMapper;
import com.mx.nqboard.export.service.ExpertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 专家管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertServiceImpl extends ServiceImpl<ExpertMapper, ExpertEntity> implements ExpertService {

	private final ExpertMapper expertMapper;
}
