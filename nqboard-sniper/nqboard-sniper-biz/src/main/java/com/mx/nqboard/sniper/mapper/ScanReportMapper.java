package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.ScanReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 扫描报告头 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface ScanReportMapper extends BaseMapper<ScanReportEntity> {
}
