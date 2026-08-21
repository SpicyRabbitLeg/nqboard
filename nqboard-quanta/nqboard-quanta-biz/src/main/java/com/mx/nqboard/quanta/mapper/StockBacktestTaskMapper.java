package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.StockBacktestTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 回测任务 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface StockBacktestTaskMapper extends BaseMapper<StockBacktestTaskEntity> {

}
