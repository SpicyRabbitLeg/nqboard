package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockCandidateEntity;
import com.mx.nqboard.quanta.service.StockCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 候选股票池 前端控制器
 * </p>
 * <p>
 * 候选由流水线自动产生，不提供手工新增/修改/删除接口。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockCandidate")
@Tag(description = "stockCandidate", name = "候选股票池模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockCandidateController {

    private final StockCandidateService stockCandidateService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockCandidate 查询条件
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockCandidatePage(@ParameterObject Page<StockCandidateEntity> page,
                                   @ParameterObject StockCandidateEntity stockCandidate) {
        LambdaQueryWrapper<StockCandidateEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(stockCandidate.getTradeDate()), StockCandidateEntity::getTradeDate, stockCandidate.getTradeDate());
        wrapper.like(StrUtil.isNotBlank(stockCandidate.getTsCode()), StockCandidateEntity::getTsCode, stockCandidate.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockCandidate.getStatus()), StockCandidateEntity::getStatus, stockCandidate.getStatus());
        wrapper.eq(StrUtil.isNotBlank(stockCandidate.getAction()), StockCandidateEntity::getAction, stockCandidate.getAction());
        wrapper.orderByDesc(StockCandidateEntity::getTradeDate).orderByDesc(StockCandidateEntity::getScreenScore);
        return R.ok(stockCandidateService.page(page, wrapper));
    }

    /**
     * 通过条件查询候选
     * @param stockCandidate 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockCandidateEntity stockCandidate) {
        return R.ok(stockCandidateService.list(Wrappers.query(stockCandidate)));
    }

    /**
     * 手动刷新候选池（阻塞执行直至完成）
     * @param date 信号日 YYYYMMDD，为空时自动取最新交易日
     * @return 入池股票数
     */
    @Operation(summary = "刷新候选池", description = "手动触发候选池刷新（Gate 硬门 + 过期管理）")
    @SysLog("刷新候选池")
    @PostMapping("/refresh")
    public R<Integer> refresh(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockCandidateService.refreshCandidates()
                : stockCandidateService.refreshCandidates(date));
    }

    /**
     * 刷新候选池（内部接口，供 Quartz/流水线 Feign 调用）
     * @param date 信号日 YYYYMMDD，为空时自动取最新交易日
     * @return 入池股票数
     */
    @Inner
    @Operation(summary = "刷新候选池（内部）", description = "内部接口：供 Quartz/流水线调用")
    @SysLog("刷新候选池(内部)")
    @PostMapping("/syncRefresh")
    public R<Integer> syncRefresh(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockCandidateService.refreshCandidates()
                : stockCandidateService.refreshCandidates(date));
    }

}
