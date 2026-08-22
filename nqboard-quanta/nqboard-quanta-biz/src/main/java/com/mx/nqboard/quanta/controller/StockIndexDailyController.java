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
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.api.vo.StockIndexDailyExportVO;
import com.mx.nqboard.quanta.service.StockIndexDailyService;
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
 * 指数日线K线表 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockIndexDaily")
@Tag(description = "stockIndexDaily", name = "指数日线K线模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockIndexDailyController {

    private final StockIndexDailyService stockIndexDailyService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockIndexDaily 指数日线K线
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockIndexDailyPage(@ParameterObject Page<StockIndexDailyEntity> page,
                                    @ParameterObject StockIndexDailyEntity stockIndexDaily) {
        LambdaQueryWrapper<StockIndexDailyEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockIndexDaily.getIndexCode()), StockIndexDailyEntity::getIndexCode, stockIndexDaily.getIndexCode());
        wrapper.eq(StrUtil.isNotBlank(stockIndexDaily.getTradeDate()), StockIndexDailyEntity::getTradeDate, stockIndexDaily.getTradeDate());
        wrapper.orderByDesc(StockIndexDailyEntity::getTradeDate);
        return R.ok(stockIndexDailyService.page(page, wrapper));
    }

    /**
     * 通过条件查询指数日线K线
     * @param stockIndexDaily 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockIndexDailyEntity stockIndexDaily) {
        return R.ok(stockIndexDailyService.list(Wrappers.query(stockIndexDaily)));
    }

    /**
     * 新增指数日线K线
     * @param stockIndexDaily 指数日线K线
     * @return R
     */
    @Operation(summary = "新增指数日线K线", description = "新增指数日线K线")
    @SysLog("新增指数日线K线")
    @PostMapping
    @HasPermission("quanta_stockIndexDaily_add")
    public R save(@Validated @RequestBody StockIndexDailyEntity stockIndexDaily) {
        return R.ok(stockIndexDailyService.save(stockIndexDaily));
    }

    /**
     * 修改指数日线K线
     * @param stockIndexDaily 指数日线K线
     * @return R
     */
    @Operation(summary = "修改指数日线K线", description = "修改指数日线K线")
    @SysLog("修改指数日线K线")
    @PutMapping
    @HasPermission("quanta_stockIndexDaily_edit")
    public R updateById(@Validated @RequestBody StockIndexDailyEntity stockIndexDaily) {
        return R.ok(stockIndexDailyService.updateById(stockIndexDaily));
    }

    /**
     * 通过id删除指数日线K线
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除指数日线K线", description = "通过id删除指数日线K线")
    @SysLog("通过id删除指数日线K线")
    @DeleteMapping
    @HasPermission("quanta_stockIndexDaily_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockIndexDailyService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockIndexDaily 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockIndexDaily_export")
    public List<StockIndexDailyExportVO> exportExcel(StockIndexDailyEntity stockIndexDaily, Long[] ids) {
        return BeanUtil.copyToList(
                stockIndexDailyService.list(Wrappers.lambdaQuery(stockIndexDaily)
                        .in(ArrayUtil.isNotEmpty(ids), StockIndexDailyEntity::getId, ids)),
                StockIndexDailyExportVO.class);
    }

    /**
     * 从 东方财富 同步指数日线K线（支持全量/增量）
     * <p>
     * 系统内部接口：供 RemoteStockIndexDailyService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param full 是否全量同步：true=从2026-01-01起；false=仅今天；为空时取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 东方财富 同步数据", description = "同步指数日线K线（支持全量/增量）")
    @SysLog("从 东方财富 同步指数日线K线")
    @PostMapping("/sync")
    public R<Integer> syncFromEastMoney(@RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockIndexDailyService.syncFromEastMoney(full).getAffected());
    }
}
