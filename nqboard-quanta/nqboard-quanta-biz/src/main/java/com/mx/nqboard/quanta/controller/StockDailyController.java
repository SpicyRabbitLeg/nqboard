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
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.vo.StockDailyExportVO;
import com.mx.nqboard.quanta.api.vo.StockDailyKlineVO;
import com.mx.nqboard.quanta.service.StockDailyService;
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
 * Tushare日线行情 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockDaily")
@Tag(description = "stockDaily", name = "股票日线行情模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockDailyController {

    private final StockDailyService stockDailyService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockDaily 股票日线行情
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockDailyPage(@ParameterObject Page<StockDailyEntity> page,
                               @ParameterObject StockDailyEntity stockDaily) {
        LambdaQueryWrapper<StockDailyEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockDaily.getTsCode()), StockDailyEntity::getTsCode, stockDaily.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockDaily.getTradeDate()), StockDailyEntity::getTradeDate, stockDaily.getTradeDate());
        wrapper.orderByDesc(StockDailyEntity::getTradeDate);
        return R.ok(stockDailyService.page(page, wrapper));
    }

    /**
     * 通过条件查询股票日线行情
     * @param stockDaily 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockDailyEntity stockDaily) {
        return R.ok(stockDailyService.list(Wrappers.query(stockDaily)));
    }

    /**
     * 新增股票日线行情
     * @param stockDaily 股票日线行情
     * @return R
     */
    @Operation(summary = "新增股票日线行情", description = "新增股票日线行情")
    @SysLog("新增股票日线行情")
    @PostMapping
    @HasPermission("quanta_stockDaily_add")
    public R save(@Validated @RequestBody StockDailyEntity stockDaily) {
        return R.ok(stockDailyService.save(stockDaily));
    }

    /**
     * 修改股票日线行情
     * @param stockDaily 股票日线行情
     * @return R
     */
    @Operation(summary = "修改股票日线行情", description = "修改股票日线行情")
    @SysLog("修改股票日线行情")
    @PutMapping
    @HasPermission("quanta_stockDaily_edit")
    public R updateById(@Validated @RequestBody StockDailyEntity stockDaily) {
        return R.ok(stockDailyService.updateById(stockDaily));
    }

    /**
     * 通过id删除股票日线行情
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除股票日线行情", description = "通过id删除股票日线行情")
    @SysLog("通过id删除股票日线行情")
    @DeleteMapping
    @HasPermission("quanta_stockDaily_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockDailyService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockDaily 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockDaily_export")
    public List<StockDailyExportVO> exportExcel(StockDailyEntity stockDaily, Long[] ids) {
        return BeanUtil.copyToList(
                stockDailyService.list(Wrappers.lambdaQuery(stockDaily)
                        .in(ArrayUtil.isNotEmpty(ids), StockDailyEntity::getId, ids)),
                StockDailyExportVO.class);
    }

    /**
     * K线数据：按股票代码返回最新 limit 根日线（正序），供前端K线图一次拉取后本地翻页
     * @param tsCode 股票代码
     * @param limit 返回根数上限，默认 2000
     * @return K线数据列表
     */
    @Operation(summary = "K线数据", description = "按股票代码返回正序日线行情，供前端K线图使用")
    @GetMapping("/kline")
    public R<List<StockDailyKlineVO>> kline(@RequestParam String tsCode,
                                            @RequestParam(defaultValue = "2000") Integer limit) {
        return R.ok(stockDailyService.kline(tsCode, limit));
    }

    /**
     * 从 tushare 同步股票日线行情（按市场过滤，支持全量/增量）
     * <p>
     * 系统内部接口：供 RemoteStockDailyService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param market 市场类型：主板/创业板/科创板，为空时取 yml 配置 tushare.daily.market
     * @param full 是否全量同步：true=2026-01-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "从 tushare 同步数据（按市场过滤，支持全量/增量）")
    @SysLog("从 tushare 同步股票日线行情")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market,
                                      @RequestParam(value = "full", required = false) Boolean full) {
        // 保持 Feign 契约 R<Integer>（影响行数）；成功/失败明细由 @QuantSyncLog 落 quant_sync_log
        return R.ok(stockDailyService.syncFromTushare(market, full).getAffected());
    }

    /**
     * 从 tushare 回补复权因子（按交易日批量，只补 NULL 的交易日）
     * <p>
     * 系统内部接口：供 RemoteStockDailyService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @return 影响行数
     */
    @Inner
    @Operation(summary = "回补复权因子", description = "从 tushare adj_factor 接口按交易日批量回补复权因子")
    @SysLog("从 tushare 回补复权因子")
    @PostMapping("/syncAdjFactor")
    public R<Integer> syncAdjFactorFromTushare() {
        // 保持 Feign 契约 R<Integer>（影响行数）；成功/失败明细由 @QuantSyncLog 落 quant_sync_log
        return R.ok(stockDailyService.syncAdjFactorFromTushare().getAffected());
    }
}
