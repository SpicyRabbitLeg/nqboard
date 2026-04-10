package com.mx.nqboard.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.device.api.dto.IotImportModelDTO;
import com.mx.nqboard.device.api.dto.IotModelDTO;
import com.mx.nqboard.device.api.entity.IotModelEntity;
import com.mx.nqboard.device.api.vo.IotModelVO;

import java.util.List;

/**
 * <p>
 * 物模型管理 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface IotModelService extends IService<IotModelEntity> {
    /**
     * 通过通用物模型导入模型
     * @param dto dto
     * @return 成功数量
     */
    Boolean importModelByTemplate(IotImportModelDTO dto);

    /**
     * 通过产品id获取模型列表
     * @param productId 产品id
     * @param deviceId 设备id
     * @return list
     */
    List<IotModelVO> getModelByProduct(Long productId, Long deviceId);

    /**
     * 添加物模型并创建数据库字段
     *
     * @param entity entity
     * @return 成功否
     */
    Boolean saveModel(IotModelEntity entity);

    /**
     *  事务添加物模型并创建数据库字段
     *
     * @param modelDto modelDto
     * @return 成功否
     */
    Boolean transactionalSave(IotModelDTO modelDto);

    /**
     * 根据id删除物模型
     *
     * @param ids ids
     * @return 成功否
     */
    Boolean removeModelByIds(List<Long> ids);

    /**
     * 事务删除物模型并删除数据库字段
     * @param modelDto modelDto
     * @return 成功否
     */
    Boolean transactionalDelete(IotModelDTO modelDto);
}
