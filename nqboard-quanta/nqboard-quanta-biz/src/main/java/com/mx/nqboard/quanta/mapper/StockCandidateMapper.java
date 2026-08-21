package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 候选股票池 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockCandidateMapper extends BaseMapper<StockCandidateEntity> {

}
