package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Tushare股票基础信息 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockBasicMapper extends BaseMapper<StockBasicEntity> {

}
