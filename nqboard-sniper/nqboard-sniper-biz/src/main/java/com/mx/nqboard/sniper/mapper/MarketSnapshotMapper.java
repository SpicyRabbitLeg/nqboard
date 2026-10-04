package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 全市场快照 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface MarketSnapshotMapper extends BaseMapper<MarketSnapshotEntity> {
}
