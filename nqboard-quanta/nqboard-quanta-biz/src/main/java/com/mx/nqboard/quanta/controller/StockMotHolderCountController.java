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
import com.mx.nqboard.quanta.api.entity.StockMotHolderCountEntity;
import com.mx.nqboard.quanta.api.vo.StockMotHolderCountExportVO;
import com.mx.nqboard.quanta.service.StockMotHolderCountService;
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
 * 股东户数表 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockMotHolderCount")
@Tag(description = "stockMotHolderCount", name = "股东户数模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockMotHolderCountController {

    private final StockMotHolderCountService stockMotHolderCountService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockMotHolderCount 股东户数
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockMotHolderCountPage(@ParameterObject Page<StockMotHolderCountEntity> page,
                                        @ParameterObject StockMotHolderCountEntity stockMotHolderCount) {
        LambdaQueryWrapper<StockMotHolderCountEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockMotHolderCount.getTsCode()), StockMotHolderCountEntity::getTsCode, stockMotHolderCount.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockMotHolderCount.getAnnDate()), StockMotHolderCountEntity::getAnnDate, stockMotHolderCount.getAnnDate());
        wrapper.eq(StrUtil.isNotBlank(stockMotHolderCount.getEndDate()), StockMotHolderCountEntity::getEndDate, stockMotHolderCount.getEndDate());
        wrapper.orderByDesc(StockMotHolderCountEntity::getEndDate);
        return R.ok(stockMotHolderCountService.page(page, wrapper));
    }

    /**
     * 通过条件查询股东户数
     * @param stockMotHolderCount 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockMotHolderCountEntity stockMotHolderCount) {
        return R.ok(stockMotHolderCountService.list(Wrappers.query(stockMotHolderCount)));
    }

    /**
     * 新增股东户数
     * @param stockMotHolderCount 股东户数
     * @return R
     */
    @Operation(summary = "新增股东户数", description = "新增股东户数")
    @SysLog("新增股东户数")
    @PostMapping
    @HasPermission("quanta_stockMotHolderCount_add")
    public R save(@Validated @RequestBody StockMotHolderCountEntity stockMotHolderCount) {
        return R.ok(stockMotHolderCountService.save(stockMotHolderCount));
    }

    /**
     * 修改股东户数
     * @param stockMotHolderCount 股东户数
     * @return R
     */
    @Operation(summary = "修改股东户数", description = "修改股东户数")
    @SysLog("修改股东户数")
    @PutMapping
    @HasPermission("quanta_stockMotHolderCount_edit")
    public R updateById(@Validated @RequestBody StockMotHolderCountEntity stockMotHolderCount) {
        return R.ok(stockMotHolderCountService.updateById(stockMotHolderCount));
    }

    /**
     * 通过id删除股东户数
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除股东户数", description = "通过id删除股东户数")
    @SysLog("通过id删除股东户数")
    @DeleteMapping
    @HasPermission("quanta_stockMotHolderCount_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockMotHolderCountService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockMotHolderCount 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockMotHolderCount_export")
    public List<StockMotHolderCountExportVO> exportExcel(StockMotHolderCountEntity stockMotHolderCount, Long[] ids) {
        return BeanUtil.copyToList(
                stockMotHolderCountService.list(Wrappers.lambdaQuery(stockMotHolderCount)
                        .in(ArrayUtil.isNotEmpty(ids), StockMotHolderCountEntity::getId, ids)),
                StockMotHolderCountExportVO.class);
    }

    /**
     * 从 tushare 同步股东户数（按市场过滤，支持全量/增量）
     * <p>
     * 系统内部接口：供 RemoteStockMotHolderCountService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param market 市场类型：主板/创业板/科创板，为空时取 yml 配置 tushare.daily.market
     * @param full 是否全量同步：true=2026-01-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "从 tushare 同步数据（按市场过滤，支持全量/增量）")
    @SysLog("从 tushare 同步股东户数")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market,
                                      @RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockMotHolderCountService.syncFromTushare(market, full).getAffected());
    }
}
