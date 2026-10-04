package com.mx.nqboard.sniper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 股票基础信息 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface StockBasicMapper extends BaseMapper<StockBasicEntity> {
}
