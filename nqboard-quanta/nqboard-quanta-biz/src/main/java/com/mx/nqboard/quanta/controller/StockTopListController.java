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
import com.mx.nqboard.quanta.api.entity.StockTopListEntity;
import com.mx.nqboard.quanta.api.vo.StockTopListExportVO;
import com.mx.nqboard.quanta.service.StockTopListService;
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
 * Tushare top_list 龙虎榜每日明细 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockTopList")
@Tag(description = "stockTopList", name = "龙虎榜每日明细模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockTopListController {

    private final StockTopListService stockTopListService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockTopList 龙虎榜每日明细
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockTopListPage(@ParameterObject Page<StockTopListEntity> page,
                                 @ParameterObject StockTopListEntity stockTopList) {
        LambdaQueryWrapper<StockTopListEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(stockTopList.getTradeDate()), StockTopListEntity::getTradeDate, stockTopList.getTradeDate());
        wrapper.like(StrUtil.isNotBlank(stockTopList.getTsCode()), StockTopListEntity::getTsCode, stockTopList.getTsCode());
        wrapper.like(StrUtil.isNotBlank(stockTopList.getName()), StockTopListEntity::getName, stockTopList.getName());
        wrapper.orderByDesc(StockTopListEntity::getTradeDate);
        return R.ok(stockTopListService.page(page, wrapper));
    }

    /**
     * 通过条件查询龙虎榜每日明细
     * @param stockTopList 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockTopListEntity stockTopList) {
        return R.ok(stockTopListService.list(Wrappers.query(stockTopList)));
    }

    /**
     * 新增龙虎榜每日明细
     * @param stockTopList 龙虎榜每日明细
     * @return R
     */
    @Operation(summary = "新增龙虎榜每日明细", description = "新增龙虎榜每日明细")
    @SysLog("新增龙虎榜每日明细")
    @PostMapping
    @HasPermission("quanta_stockTopList_add")
    public R save(@Validated @RequestBody StockTopListEntity stockTopList) {
        return R.ok(stockTopListService.save(stockTopList));
    }

    /**
     * 修改龙虎榜每日明细
     * @param stockTopList 龙虎榜每日明细
     * @return R
     */
    @Operation(summary = "修改龙虎榜每日明细", description = "修改龙虎榜每日明细")
    @SysLog("修改龙虎榜每日明细")
    @PutMapping
    @HasPermission("quanta_stockTopList_edit")
    public R updateById(@Validated @RequestBody StockTopListEntity stockTopList) {
        return R.ok(stockTopListService.updateById(stockTopList));
    }

    /**
     * 通过id删除龙虎榜每日明细
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除龙虎榜每日明细", description = "通过id删除龙虎榜每日明细")
    @SysLog("通过id删除龙虎榜每日明细")
    @DeleteMapping
    @HasPermission("quanta_stockTopList_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockTopListService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockTopList 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockTopList_export")
    public List<StockTopListExportVO> exportExcel(StockTopListEntity stockTopList, Long[] ids) {
        return BeanUtil.copyToList(
                stockTopListService.list(Wrappers.lambdaQuery(stockTopList)
                        .in(ArrayUtil.isNotEmpty(ids), StockTopListEntity::getId, ids)),
                StockTopListExportVO.class);
    }

    /**
     * 从 tushare 同步龙虎榜每日明细（按日期遍历，支持全量/增量/指定日期）
     * <p>
     * 系统内部接口：供 RemoteStockTopListService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param tradeDate 指定交易日期 YYYYMMDD（可空；为空时按 full 决定范围）
     * @param full 是否全量同步：true=2026-01-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "从 tushare 同步数据（按日期遍历，支持全量/增量/指定日期）")
    @SysLog("从 tushare 同步龙虎榜每日明细")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "tradeDate", required = false) String tradeDate,
                                      @RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockTopListService.syncFromTushare(tradeDate, full).getAffected());
    }
}
