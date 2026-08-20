package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockMotHolderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Tushare 股东增减持表 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockMotHolderMapper extends BaseMapper<StockMotHolderEntity> {

}
