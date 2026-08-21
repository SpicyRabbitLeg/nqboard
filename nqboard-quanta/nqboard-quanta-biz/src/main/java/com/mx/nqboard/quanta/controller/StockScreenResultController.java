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
import com.mx.nqboard.quanta.api.entity.StockScreenResultEntity;
import com.mx.nqboard.quanta.api.vo.StockScreenResultExportVO;
import com.mx.nqboard.quanta.service.StockScreenResultService;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 每日筛选打分结果 前端控制器
 * </p>
 * <p>
 * 打分结果由系统自动生成，不提供手工新增/修改/删除接口。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockScreenResult")
@Tag(description = "stockScreenResult", name = "每日筛选打分模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockScreenResultController {

    private final StockScreenResultService stockScreenResultService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockScreenResult 筛选结果
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockScreenResultPage(@ParameterObject Page<StockScreenResultEntity> page,
                                      @ParameterObject StockScreenResultEntity stockScreenResult) {
        LambdaQueryWrapper<StockScreenResultEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(stockScreenResult.getTradeDate()), StockScreenResultEntity::getTradeDate, stockScreenResult.getTradeDate());
        wrapper.like(StrUtil.isNotBlank(stockScreenResult.getTsCode()), StockScreenResultEntity::getTsCode, stockScreenResult.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockScreenResult.getPattern()), StockScreenResultEntity::getPattern, stockScreenResult.getPattern());
        wrapper.eq(StrUtil.isNotBlank(stockScreenResult.getPassed()), StockScreenResultEntity::getPassed, stockScreenResult.getPassed());
        wrapper.orderByDesc(StockScreenResultEntity::getTradeDate).orderByDesc(StockScreenResultEntity::getScreenScore);
        return R.ok(stockScreenResultService.page(page, wrapper));
    }

    /**
     * 通过条件查询筛选结果
     * @param stockScreenResult 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockScreenResultEntity stockScreenResult) {
        return R.ok(stockScreenResultService.list(Wrappers.query(stockScreenResult)));
    }

    /**
     * 查询指定信号日通过入池线的 TopN 候选
     * @param tradeDate 信号日 YYYYMMDD
     * @param topN 最大数量，默认取配置 TopN
     * @return 候选列表
     */
    @Operation(summary = "TopN候选", description = "指定信号日通过入池线的TopN候选（按打分降序）")
    @GetMapping("/topCandidates")
    public R topCandidates(@RequestParam(value = "tradeDate") String tradeDate,
                           @RequestParam(value = "topN", required = false, defaultValue = "3") Integer topN) {
        return R.ok(stockScreenResultService.topCandidates(tradeDate, topN));
    }

    /**
     * 导出excel 表格
     * @param stockScreenResult 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockScreenResult_export")
    public List<StockScreenResultExportVO> exportExcel(StockScreenResultEntity stockScreenResult, Long[] ids) {
        return BeanUtil.copyToList(
                stockScreenResultService.list(Wrappers.lambdaQuery(stockScreenResult)
                        .in(ArrayUtil.isNotEmpty(ids), StockScreenResultEntity::getId, ids)),
                StockScreenResultExportVO.class);
    }

    /**
     * 触发筛选打分（阻塞执行直至完成）
     * <p>
     * 系统内部接口：供 RemoteStockScreenResultService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @param date 信号日 YYYYMMDD，为空时自动取指数日线最新交易日
     * @return 处理的股票数
     */
    @Inner
    @Operation(summary = "触发筛选打分", description = "执行筛选打分（可指定信号日）")
    @SysLog("触发筛选打分")
    @PostMapping("/screen")
    public R<Integer> screen(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockScreenResultService.screen() : stockScreenResultService.screen(date));
    }

}
