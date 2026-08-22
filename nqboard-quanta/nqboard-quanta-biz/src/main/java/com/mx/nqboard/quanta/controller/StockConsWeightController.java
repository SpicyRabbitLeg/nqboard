package com.mx.nqboard.quanta.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockConsWeightEntity;
import com.mx.nqboard.quanta.api.vo.StockConsWeightExportVO;
import com.mx.nqboard.quanta.service.StockConsWeightService;
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
 * 指数成分股及权重 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockConsWeight")
@Tag(description = "stockConsWeight", name = "指数成分股及权重模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockConsWeightController {

    private final StockConsWeightService stockConsWeightService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockConsWeight 指数成分股及权重
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockConsWeightPage(@ParameterObject Page<StockConsWeightEntity> page,
                                    @ParameterObject StockConsWeightEntity stockConsWeight) {
        LambdaQueryWrapper<StockConsWeightEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockConsWeight.getTsCode()), StockConsWeightEntity::getTsCode, stockConsWeight.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockConsWeight.getIndexCode()), StockConsWeightEntity::getIndexCode, stockConsWeight.getIndexCode());
        wrapper.eq(stockConsWeight.getTradeDate() != null, StockConsWeightEntity::getTradeDate, stockConsWeight.getTradeDate());
        wrapper.orderByDesc(StockConsWeightEntity::getTradeDate);
        return R.ok(stockConsWeightService.page(page, wrapper));
    }

    /**
     * 通过条件查询指数成分股及权重
     * @param stockConsWeight 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockConsWeightEntity stockConsWeight) {
        return R.ok(stockConsWeightService.list(Wrappers.query(stockConsWeight)));
    }

    /**
     * 新增指数成分股及权重
     * @param stockConsWeight 指数成分股及权重
     * @return R
     */
    @Operation(summary = "新增指数成分股及权重", description = "新增指数成分股及权重")
    @SysLog("新增指数成分股及权重")
    @PostMapping
    @HasPermission("quanta_stockConsWeight_add")
    public R save(@Validated @RequestBody StockConsWeightEntity stockConsWeight) {
        return R.ok(stockConsWeightService.save(stockConsWeight));
    }

    /**
     * 修改指数成分股及权重
     * @param stockConsWeight 指数成分股及权重
     * @return R
     */
    @Operation(summary = "修改指数成分股及权重", description = "修改指数成分股及权重")
    @SysLog("修改指数成分股及权重")
    @PutMapping
    @HasPermission("quanta_stockConsWeight_edit")
    public R updateById(@Validated @RequestBody StockConsWeightEntity stockConsWeight) {
        return R.ok(stockConsWeightService.updateById(stockConsWeight));
    }

    /**
     * 通过id删除指数成分股及权重
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除指数成分股及权重", description = "通过id删除指数成分股及权重")
    @SysLog("通过id删除指数成分股及权重")
    @DeleteMapping
    @HasPermission("quanta_stockConsWeight_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockConsWeightService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockConsWeight 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockConsWeight_export")
    public List<StockConsWeightExportVO> exportExcel(StockConsWeightEntity stockConsWeight, Long[] ids) {
        return BeanUtil.copyToList(
                stockConsWeightService.list(Wrappers.lambdaQuery(stockConsWeight)
                        .in(ArrayUtil.isNotEmpty(ids), StockConsWeightEntity::getId, ids)),
                StockConsWeightExportVO.class);
    }

    /**
     * 从 中证指数官网 同步指数成分股权重
     * <p>
     * 系统内部接口：供 RemoteStockConsWeightService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param filePath 本地 xls 文件路径（可空；为空则按 yml 配置的指数列表从官网下载）
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 中证指数官网 同步数据", description = "同步指数成分股权重（可指定本地xls文件）")
    @SysLog("从 中证指数官网 同步指数成分股权重")
    @PostMapping("/sync")
    public R<Integer> syncFromCsindex(@RequestParam(value = "filePath", required = false) String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return R.ok(stockConsWeightService.syncFromCsindex().getAffected());
        }
        return R.ok(stockConsWeightService.syncFromCsindex(filePath).getAffected());
    }
}
