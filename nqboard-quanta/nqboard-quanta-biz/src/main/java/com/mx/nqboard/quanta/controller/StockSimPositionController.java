package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockPositionDailyEntity;
import com.mx.nqboard.quanta.api.entity.StockSimPositionEntity;
import com.mx.nqboard.quanta.service.StockPositionDailyService;
import com.mx.nqboard.quanta.service.StockSimPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 模拟持仓 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockSimPosition")
@Tag(description = "stockSimPosition", name = "模拟持仓模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockSimPositionController {

    private final StockSimPositionService stockSimPositionService;

    private final StockPositionDailyService stockPositionDailyService;

    /**
     * 分页查询持仓
     * @param page 分页对象
     * @param stockSimPosition 查询条件
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockSimPositionPage(@ParameterObject Page<StockSimPositionEntity> page,
                                     @ParameterObject StockSimPositionEntity stockSimPosition) {
        LambdaQueryWrapper<StockSimPositionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(stockSimPosition.getStatus()), StockSimPositionEntity::getStatus, stockSimPosition.getStatus());
        wrapper.like(StrUtil.isNotBlank(stockSimPosition.getTsCode()), StockSimPositionEntity::getTsCode, stockSimPosition.getTsCode());
        wrapper.orderByDesc(StockSimPositionEntity::getBuyDate);
        return R.ok(stockSimPositionService.page(page, wrapper));
    }

    /**
     * 持仓详情
     * @param id 持仓id
     * @return 持仓实体
     */
    @Operation(summary = "持仓详情", description = "持仓详情")
    @GetMapping("/details")
    public R getDetails(@RequestParam("id") Long id) {
        return R.ok(stockSimPositionService.getById(id));
    }

    /**
     * 模拟买入（创建计划委托，次日开盘价成交）
     * @param candidateId 候选记录id
     * @return 持仓记录id
     */
    @Operation(summary = "模拟买入", description = "创建计划委托，信号日次一交易日开盘价成交（T+1真实化）")
    @SysLog("模拟买入")
    @PostMapping("/buy")
    public R<Long> buy(@RequestParam("candidateId") Long candidateId) {
        return R.ok(stockSimPositionService.createPendingBuy(candidateId));
    }

    /**
     * 持仓逐日盯市记录
     * @param positionId 持仓id
     * @return 盯市记录列表（按交易日升序）
     */
    @Operation(summary = "逐日盯市记录", description = "持仓逐日盯市记录（含每日动作建议）")
    @GetMapping("/daily")
    public R getDaily(@RequestParam("positionId") Long positionId) {
        return R.ok(stockPositionDailyService.list(Wrappers.<StockPositionDailyEntity>lambdaQuery()
                .eq(StockPositionDailyEntity::getPositionId, positionId)
                .orderByAsc(StockPositionDailyEntity::getTradeDate)));
    }

    /**
     * 持仓总览（活跃持仓数/已实现盈亏/浮动盈亏）
     * @return 总览统计
     */
    @Operation(summary = "持仓总览", description = "活跃持仓数/已实现盈亏/浮动盈亏统计")
    @GetMapping("/overview")
    public R getOverview() {
        List<StockSimPositionEntity> all = stockSimPositionService
                .list(Wrappers.<StockSimPositionEntity>lambdaQuery()
                        .in(StockSimPositionEntity::getStatus, "PENDING_BUY", "HOLDING", "PENDING_SELL", "EXITED"));
        long active = all.stream().filter(p -> !"EXITED".equals(p.getStatus()) && !"CANCELLED".equals(p.getStatus())).count();
        double realized = all.stream()
                .filter(p -> "EXITED".equals(p.getStatus()) && p.getPnl() != null)
                .mapToDouble(p -> p.getPnl().doubleValue())
                .sum();
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("activePositions", active);
        overview.put("realizedPnl", Math.round(realized * 100) / 100.0);
        // 浮动盈亏需结合最新收盘价，前端可通过 /daily 明细获取精确值
        overview.put("floatingPnlNote", "详见各持仓 /daily 逐日盯市 cumPnl");
        return R.ok(overview);
    }

    /**
     * 手动触发持仓跟踪（收盘数据就绪后）
     * @param date 交易日 YYYYMMDD，为空时自动取最新交易日
     * @return 处理的持仓数
     */
    @Operation(summary = "持仓跟踪", description = "手动触发当日持仓跟踪（买入成交/离场评估/逐日盯市）")
    @SysLog("持仓跟踪")
    @PostMapping("/track")
    public R<Integer> track(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockSimPositionService.trackPositions()
                : stockSimPositionService.trackPositions(date));
    }

    /**
     * 持仓跟踪（内部接口，供 Quartz/流水线 Feign 调用）
     * @param date 交易日 YYYYMMDD，为空时自动取最新交易日
     * @return 处理的持仓数
     */
    @Inner
    @Operation(summary = "持仓跟踪（内部）", description = "内部接口：供 Quartz/流水线调用")
    @SysLog("持仓跟踪(内部)")
    @PostMapping("/syncTrack")
    public R<Integer> syncTrack(@RequestParam(value = "date", required = false) String date) {
        return R.ok(StrUtil.isBlank(date) ? stockSimPositionService.trackPositions()
                : stockSimPositionService.trackPositions(date));
    }

}
