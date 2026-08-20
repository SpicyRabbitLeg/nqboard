package com.mx.nqboard.quanta.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockBasicEntity;
import com.mx.nqboard.quanta.api.vo.StockBasicExportVO;
import com.mx.nqboard.quanta.api.vo.StockOptionVO;
import com.mx.nqboard.quanta.service.StockBasicService;
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
 * Tushare股票基础信息 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockBasic")
@Tag(description = "stockBasic", name = "股票基础信息模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockBasicController {

    private final StockBasicService stockBasicService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockBasic 股票基础信息
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockBasicPage(@ParameterObject Page<StockBasicEntity> page,
                               @ParameterObject StockBasicEntity stockBasic) {
        LambdaQueryWrapper<StockBasicEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockBasic.getName()), StockBasicEntity::getName, stockBasic.getName());
        wrapper.like(StrUtil.isNotBlank(stockBasic.getTsCode()), StockBasicEntity::getTsCode, stockBasic.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockBasic.getMarket()), StockBasicEntity::getMarket, stockBasic.getMarket());
        return R.ok(stockBasicService.page(page, wrapper));
    }

    /**
     * 通过条件查询股票基础信息
     * @param stockBasic 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockBasicEntity stockBasic) {
        return R.ok(stockBasicService.list(Wrappers.query(stockBasic)));
    }

    /**
     * 新增股票基础信息
     * @param stockBasic 股票基础信息
     * @return R
     */
    @Operation(summary = "新增股票基础信息", description = "新增股票基础信息")
    @SysLog("新增股票基础信息")
    @PostMapping
    @HasPermission("quanta_stockBasic_add")
    public R save(@Validated @RequestBody StockBasicEntity stockBasic) {
        return R.ok(stockBasicService.save(stockBasic));
    }

    /**
     * 修改股票基础信息
     * @param stockBasic 股票基础信息
     * @return R
     */
    @Operation(summary = "修改股票基础信息", description = "修改股票基础信息")
    @SysLog("修改股票基础信息")
    @PutMapping
    @HasPermission("quanta_stockBasic_edit")
    public R updateById(@Validated @RequestBody StockBasicEntity stockBasic) {
        return R.ok(stockBasicService.updateById(stockBasic));
    }

    /**
     * 通过id删除股票基础信息
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除股票基础信息", description = "通过id删除股票基础信息")
    @SysLog("通过id删除股票基础信息")
    @DeleteMapping
    @HasPermission("quanta_stockBasic_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockBasicService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockBasic 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockBasic_export")
    public List<StockBasicExportVO> exportExcel(StockBasicEntity stockBasic, Long[] ids) {
        return BeanUtil.copyToList(
                stockBasicService.list(Wrappers.lambdaQuery(stockBasic)
                        .in(ArrayUtil.isNotEmpty(ids), StockBasicEntity::getId, ids)),
                StockBasicExportVO.class);
    }

    /**
     * 股票下拉选项分页查询（精简 tsCode/name）
     * <p>
     * 前端下拉 remote 搜索 + 滚动到底自动加载下一页；keyword 按代码/名称模糊匹配
     * @param keyword 关键字，可空（空则查询全部）
     * @param current 页码，从 1 开始
     * @param size 每页条数
     * @return 下拉选项分页结果
     */
    @Operation(summary = "股票下拉选项", description = "精简返回股票代码/名称，支持代码或名称模糊搜索，分页返回")
    @GetMapping("/options")
    public R<IPage<StockOptionVO>> options(@RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "current", defaultValue = "1") long current,
                                           @RequestParam(value = "size", defaultValue = "50") long size) {
        return R.ok(stockBasicService.options(keyword, current, size));
    }

    /**
     * 从 tushare 同步股票基础信息（按市场）
     * <p>
     * 系统内部接口：供 RemoteStockBasicService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param market 市场类型：主板/创业板/科创板，为空时同步 "主板"
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 tushare 同步数据", description = "从 tushare 同步数据（按市场）")
    @SysLog("从 tushare 同步股票基础信息")
    @PostMapping("/sync")
    public R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market) {
        return R.ok(stockBasicService.syncFromTushare(market));
    }
}
