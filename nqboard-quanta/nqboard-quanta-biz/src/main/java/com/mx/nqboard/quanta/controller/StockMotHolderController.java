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
import com.mx.nqboard.quanta.api.entity.StockMotHolderEntity;
import com.mx.nqboard.quanta.api.vo.StockMotHolderExportVO;
import com.mx.nqboard.quanta.service.StockMotHolderService;
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
 * Tushare 股东增减持表 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockMotHolder")
@Tag(description = "stockMotHolder", name = "股东增减持模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockMotHolderController {

    private final StockMotHolderService stockMotHolderService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockMotHolder 股东增减持
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockMotHolderPage(@ParameterObject Page<StockMotHolderEntity> page,
                                   @ParameterObject StockMotHolderEntity stockMotHolder) {
        LambdaQueryWrapper<StockMotHolderEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockMotHolder.getTsCode()), StockMotHolderEntity::getTsCode, stockMotHolder.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockMotHolder.getAnnDate()), StockMotHolderEntity::getAnnDate, stockMotHolder.getAnnDate());
        wrapper.like(StrUtil.isNotBlank(stockMotHolder.getHolderName()), StockMotHolderEntity::getHolderName, stockMotHolder.getHolderName());
        wrapper.orderByDesc(StockMotHolderEntity::getAnnDate);
        return R.ok(stockMotHolderService.page(page, wrapper));
    }

    /**
     * 通过条件查询股东增减持
     * @param stockMotHolder 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockMotHolderEntity stockMotHolder) {
        return R.ok(stockMotHolderService.list(Wrappers.query(stockMotHolder)));
    }

    /**
     * 新增股东增减持
     * @param stockMotHolder 股东增减持
     * @return R
     */
    @Operation(summary = "新增股东增减持", description = "新增股东增减持")
    @SysLog("新增股东增减持")
    @PostMapping
    @HasPermission("quanta_stockMotHolder_add")
    public R save(@Validated @RequestBody StockMotHolderEntity stockMotHolder) {
        return R.ok(stockMotHolderService.save(stockMotHolder));
    }

    /**
     * 修改股东增减持
     * @param stockMotHolder 股东增减持
     * @return R
     */
    @Operation(summary = "修改股东增减持", description = "修改股东增减持")
    @SysLog("修改股东增减持")
    @PutMapping
    @HasPermission("quanta_stockMotHolder_edit")
    public R updateById(@Validated @RequestBody StockMotHolderEntity stockMotHolder) {
        return R.ok(stockMotHolderService.updateById(stockMotHolder));
    }

    /**
     * 通过id删除股东增减持
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除股东增减持", description = "通过id删除股东增减持")
    @SysLog("通过id删除股东增减持")
    @DeleteMapping
    @HasPermission("quanta_stockMotHolder_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockMotHolderService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockMotHolder 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockMotHolder_export")
    public List<StockMotHolderExportVO> exportExcel(StockMotHolderEntity stockMotHolder, Long[] ids) {
        return BeanUtil.copyToList(
                stockMotHolderService.list(Wrappers.lambdaQuery(stockMotHolder)
                        .in(ArrayUtil.isNotEmpty(ids), StockMotHolderEntity::getId, ids)),
                StockMotHolderExportVO.class);
    }

    /**
     * 从 tushare 同步股东增减持（按市场过滤，支持全量/增量）
     * <p>
     * 系统内部接口：供 RemoteStockMotHolderService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param market 市场类型：主板/创业板/科创板，为空时取 yml 配置 tushare.daily.market
     * @param full 是否全量同步：true=2026-08-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "从 tushare 同步数据（按市场过滤，支持全量/增量）")
    @SysLog("从 tushare 同步股东增减持")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market,
                                      @RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockMotHolderService.syncFromTushare(market, full));
    }
}
