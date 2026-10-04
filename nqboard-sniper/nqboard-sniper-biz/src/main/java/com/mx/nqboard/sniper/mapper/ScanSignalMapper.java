package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.ScanSignalEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 扫描信号明细 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface ScanSignalMapper extends BaseMapper<ScanSignalEntity> {
}
