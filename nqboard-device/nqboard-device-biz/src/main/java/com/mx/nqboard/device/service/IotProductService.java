package com.mx.nqboard.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.device.api.dto.IotProductDTO;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotProductVO;

/**
 * <p>
 * 产品管理 服务类
 * </p>
 *
 * @author 泥鳅压滑板
 */
public interface IotProductService extends IService<IotProductEntity> {

    /**
     * 分页查询
     * @param page 分页查询条件
     * @param iotProduct iotProduct
     * @return 分页对象
     */
    IPage<IotProductVO> page(Page<IotProductEntity> page, IotProductEntity iotProduct);

    /**
     * 新增产品
     *
     * @param iotProduct 产品表
     * @return R
     */
    Boolean saveProduct(IotProductEntity iotProduct);


    /**
     * 事务保存产品
     * @param iotProduct 产品表
     * @return 成功否
     */
    Boolean transactionalSave(IotProductDTO iotProduct);
}
