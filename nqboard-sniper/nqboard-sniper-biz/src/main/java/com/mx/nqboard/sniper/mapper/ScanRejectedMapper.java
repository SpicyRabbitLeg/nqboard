package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.ScanRejectedEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 扫描被拒明细 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface ScanRejectedMapper extends BaseMapper<ScanRejectedEntity> {
}
