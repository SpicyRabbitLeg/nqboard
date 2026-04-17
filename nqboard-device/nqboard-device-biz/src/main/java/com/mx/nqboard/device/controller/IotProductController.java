package com.mx.nqboard.device.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotProductEntityExportVO;
import com.mx.nqboard.device.service.IotProductService;
import com.mx.nqboard.device.utils.ChineseUtils;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 产品管理 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(description = "product", name = "产品管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class IotProductController {

    private final IotProductService iotProductService;

    /**
     * 分页查询
     *
     * @param page       分页对象
     * @param iotProduct 产品表
     * @return R
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getIotProductPage(@ParameterObject Page page, @ParameterObject IotProductEntity iotProduct) {
        return R.ok(iotProductService.page(page, iotProduct));
    }


    /**
     * 通过条件查询产品表
     *
     * @param iotProduct 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject IotProductEntity iotProduct) {
        return R.ok(iotProductService.list(Wrappers.query(iotProduct)));
    }

    /**
     * 通过id查询
     *
     * @param id id
     * @return R 对象
     */
    @Operation(summary = "通过id查询", description = "通过id查询")
    @GetMapping("/detail/{id}")
    public R getDetailById(@ParameterObject @PathVariable Long id) {
        return R.ok(iotProductService.getById(id));
    }

    /**
     * 新增产品表
     *
     * @param iotProduct 产品表
     * @return R
     */
    @Operation(summary = "新增产品表", description = "新增产品表")
    @SysLog("新增产品表")
    @PostMapping
    @HasPermission("device_product_add")
    public R save(@Validated @RequestBody IotProductEntity iotProduct) {
        return R.ok(iotProductService.saveProduct(iotProduct));
    }

    /**
     * 修改产品表
     *
     * @param iotProduct 产品表
     * @return R
     */
    @Operation(summary = "修改产品表", description = "修改产品表")
    @SysLog("修改产品表")
    @PutMapping
    @HasPermission("device_product_edit")
    public R updateById(@RequestBody IotProductEntity iotProduct) {
        // 不允许修改唯一标识
        iotProduct.setIdentifier(null);
        return R.ok(iotProductService.updateById(iotProduct));
    }

    /**
     * 通过id删除产品表
     *
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除产品表", description = "通过id删除产品表")
    @SysLog("通过id删除产品表")
    @DeleteMapping
    @HasPermission("device_product_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(iotProductService.removeBatchByIds(CollUtil.toList(ids)));
    }


    /**
     * 导出excel 表格
     *
     * @param iotProduct 查询条件
     * @param ids        导出指定ID
     * @return excel 文件流
     */
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("device_product_export")
    public List<IotProductEntityExportVO> exportExcel(IotProductEntity iotProduct, Long[] ids) {
        return BeanUtil.copyToList(iotProductService.list(Wrappers.lambdaQuery(iotProduct).in(ArrayUtil.isNotEmpty(ids), IotProductEntity::getId, ids)), IotProductEntityExportVO.class);
    }

    /**
     * 根据中文获取拼音
     *
     * @param word word
     * @return 拼音
     */
    @Operation(summary = "根据中文获取拼音",description = "根据中文获取拼音")
    @GetMapping("/getPingYing")
    public R getPingYing(String word) {
        return R.ok(ChineseUtils.getWord(word));
    }
}
