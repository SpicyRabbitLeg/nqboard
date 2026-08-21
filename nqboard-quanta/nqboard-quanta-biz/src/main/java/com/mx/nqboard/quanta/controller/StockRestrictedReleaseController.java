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
import com.mx.nqboard.quanta.api.entity.StockRestrictedReleaseEntity;
import com.mx.nqboard.quanta.api.vo.StockRestrictedReleaseExportVO;
import com.mx.nqboard.quanta.service.StockRestrictedReleaseService;
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
 * 限售解禁 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockRestrictedRelease")
@Tag(description = "stockRestrictedRelease", name = "限售解禁模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockRestrictedReleaseController {

    private final StockRestrictedReleaseService stockRestrictedReleaseService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockRestrictedRelease 限售解禁
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockRestrictedReleasePage(@ParameterObject Page<StockRestrictedReleaseEntity> page,
                                           @ParameterObject StockRestrictedReleaseEntity stockRestrictedRelease) {
        LambdaQueryWrapper<StockRestrictedReleaseEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockRestrictedRelease.getTsCode()), StockRestrictedReleaseEntity::getTsCode, stockRestrictedRelease.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockRestrictedRelease.getFloatDate()), StockRestrictedReleaseEntity::getFloatDate, stockRestrictedRelease.getFloatDate());
        wrapper.orderByDesc(StockRestrictedReleaseEntity::getFloatDate);
        return R.ok(stockRestrictedReleaseService.page(page, wrapper));
    }

    /**
     * 通过条件查询限售解禁
     * @param stockRestrictedRelease 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockRestrictedReleaseEntity stockRestrictedRelease) {
        return R.ok(stockRestrictedReleaseService.list(Wrappers.query(stockRestrictedRelease)));
    }

    /**
     * 新增限售解禁
     * @param stockRestrictedRelease 限售解禁
     * @return R
     */
    @Operation(summary = "新增限售解禁", description = "新增限售解禁")
    @SysLog("新增限售解禁")
    @PostMapping
    @HasPermission("quanta_stockRestrictedRelease_add")
    public R save(@Validated @RequestBody StockRestrictedReleaseEntity stockRestrictedRelease) {
        return R.ok(stockRestrictedReleaseService.save(stockRestrictedRelease));
    }

    /**
     * 修改限售解禁
     * @param stockRestrictedRelease 限售解禁
     * @return R
     */
    @Operation(summary = "修改限售解禁", description = "修改限售解禁")
    @SysLog("修改限售解禁")
    @PutMapping
    @HasPermission("quanta_stockRestrictedRelease_edit")
    public R updateById(@Validated @RequestBody StockRestrictedReleaseEntity stockRestrictedRelease) {
        return R.ok(stockRestrictedReleaseService.updateById(stockRestrictedRelease));
    }

    /**
     * 通过id删除限售解禁
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除限售解禁", description = "通过id删除限售解禁")
    @SysLog("通过id删除限售解禁")
    @DeleteMapping
    @HasPermission("quanta_stockRestrictedRelease_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockRestrictedReleaseService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockRestrictedRelease 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockRestrictedRelease_export")
    public List<StockRestrictedReleaseExportVO> exportExcel(StockRestrictedReleaseEntity stockRestrictedRelease, Long[] ids) {
        return BeanUtil.copyToList(
                stockRestrictedReleaseService.list(Wrappers.lambdaQuery(stockRestrictedRelease)
                        .in(ArrayUtil.isNotEmpty(ids), StockRestrictedReleaseEntity::getId, ids)),
                StockRestrictedReleaseExportVO.class);
    }

    /**
     * 从 tushare 同步限售解禁数据（支持增量/全量回补）
     * <p>
     * 系统内部接口：供 RemoteStockRestrictedReleaseService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param full 是否全量回补：true=按 yml 配置的回补天数逐日拉取；false=仅今天
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "同步限售解禁（支持增量/全量回补）")
    @SysLog("从 tushare 同步限售解禁")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockRestrictedReleaseService.syncFromTushare(full));
    }

}
