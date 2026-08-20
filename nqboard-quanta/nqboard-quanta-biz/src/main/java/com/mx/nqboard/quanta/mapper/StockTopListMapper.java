package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Tushare top_list 龙虎榜每日明细 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockTopListMapper extends BaseMapper<StockTopListEntity> {

}
