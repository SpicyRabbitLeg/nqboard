package com.mx.nqboard.sniper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.sniper.api.entity.CompanyNewsEntity;
import com.mx.nqboard.sniper.mapper.CompanyNewsMapper;
import com.mx.nqboard.sniper.service.CompanyNewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 个股新闻 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
public class CompanyNewsServiceImpl extends ServiceImpl<CompanyNewsMapper, CompanyNewsEntity> implements CompanyNewsService {
}
