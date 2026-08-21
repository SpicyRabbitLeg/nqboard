package com.mx.nqboard.quanta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mx.nqboard.quanta.api.entity.QuantPipelineLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 盘后流水线执行日志 Mapper 接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Mapper
public interface QuantPipelineLogMapper extends BaseMapper<QuantPipelineLogEntity> {

}
