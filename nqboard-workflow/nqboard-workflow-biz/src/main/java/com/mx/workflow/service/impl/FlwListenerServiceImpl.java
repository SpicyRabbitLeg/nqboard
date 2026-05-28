package com.mx.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.workflow.api.entity.FlwListenerEntity;
import com.mx.workflow.mapper.FlwListenerMapper;
import com.mx.workflow.service.FlwListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 流程监听管理 服务类实现类
 * </p>
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlwListenerServiceImpl extends ServiceImpl<FlwListenerMapper, FlwListenerEntity> implements FlwListenerService {

    private final FlwListenerMapper flwListenerMapper;


}
