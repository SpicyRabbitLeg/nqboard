package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockCandidateHitEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 候选池信号命中率追踪 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockCandidateHitMapper extends BaseMapper<StockCandidateHitEntity> {

}
