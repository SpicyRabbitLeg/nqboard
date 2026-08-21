package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockAgentAnalysisEntity;
import com.mx.nqboard.quanta.service.StockAgentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * LLM逐Agent分析结果 前端控制器
 * </p>
 * <p>
 * 分析结果由流水线自动产生，不提供手工新增/修改/删除接口。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockAgentAnalysis")
@Tag(description = "stockAgentAnalysis", name = "LLM分析结果模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockAgentAnalysisController {

    private final StockAgentAnalysisService stockAgentAnalysisService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockAgentAnalysis 查询条件
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockAgentAnalysisPage(@ParameterObject Page<StockAgentAnalysisEntity> page,
                                       @ParameterObject StockAgentAnalysisEntity stockAgentAnalysis) {
        LambdaQueryWrapper<StockAgentAnalysisEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(stockAgentAnalysis.getTradeDate()), StockAgentAnalysisEntity::getTradeDate, stockAgentAnalysis.getTradeDate());
        wrapper.like(StrUtil.isNotBlank(stockAgentAnalysis.getTsCode()), StockAgentAnalysisEntity::getTsCode, stockAgentAnalysis.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockAgentAnalysis.getAgentKey()), StockAgentAnalysisEntity::getAgentKey, stockAgentAnalysis.getAgentKey());
        wrapper.eq(StrUtil.isNotBlank(stockAgentAnalysis.getDecisionMode()), StockAgentAnalysisEntity::getDecisionMode, stockAgentAnalysis.getDecisionMode());
        wrapper.orderByDesc(StockAgentAnalysisEntity::getTradeDate).orderByAsc(StockAgentAnalysisEntity::getAgentKey);
        return R.ok(stockAgentAnalysisService.page(page, wrapper));
    }

    /**
     * 手动触发 LLM 分析（阻塞执行直至完成）
     * @param date 基准日 YYYYMMDD，为空时自动取候选池最新信号日
     * @return 处理的股票数
     */
    @Operation(summary = "触发LLM分析", description = "对当日候选池逐只调用 Dify Workflow 分析")
    @SysLog("触发LLM分析")
    @PostMapping("/analyze")
    public R<Integer> analyze(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockAgentAnalysisService.analyze()
                : stockAgentAnalysisService.analyze(date));
    }

    /**
     * LLM 分析（内部接口，供 Quartz/流水线 Feign 调用）
     * @param date 基准日 YYYYMMDD，为空时自动取最新
     * @return 处理的股票数
     */
    @Inner
    @Operation(summary = "LLM分析（内部）", description = "内部接口：供 Quartz/流水线调用")
    @SysLog("LLM分析(内部)")
    @PostMapping("/syncAnalyze")
    public R<Integer> syncAnalyze(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockAgentAnalysisService.analyze()
                : stockAgentAnalysisService.analyze(date));
    }

}
