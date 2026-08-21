package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.StockCandidateHitEntity;
import com.mx.nqboard.quanta.service.StockCandidateHitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 候选池命中率日报 前端控制器
 * </p>
 * <p>
 * 试运行观察体系：信号质量追踪（前向收益）、LLM vs 规则 A/B 对比、
 * 分数区间校准。数据由流水线自动刷新，不提供手工新增/修改/删除接口。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockCandidateHit")
@Tag(description = "stockCandidateHit", name = "命中率日报模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockCandidateHitController {

    private final StockCandidateHitService stockCandidateHitService;

    /**
     * 命中率汇总（近 N 个信号日）：整体 + 按模板 + 按决策模式（LLM vs 规则 A/B）+ 按分数区间
     * @param days 回看信号日数，默认 30
     * @return 汇总 JSON
     */
    @Operation(summary = "命中率汇总", description = "整体/模板/决策模式/分数区间多维聚合（胜率口径 fwd_3d>0）")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam(value = "days", required = false, defaultValue = "30") Integer days) {
        return R.ok(stockCandidateHitService.summary(days));
    }

    /**
     * 指定信号日的候选命中明细
     * @param date 信号日 YYYYMMDD
     * @return 命中明细列表
     */
    @Operation(summary = "信号日命中明细", description = "指定信号日候选的前向收益明细")
    @GetMapping("/daily")
    public R<List<StockCandidateHitEntity>> daily(@RequestParam("date") String date) {
        return R.ok(stockCandidateHitService.daily(date));
    }

    /**
     * 手动刷新命中率追踪（回看窗口内全部信号日，幂等）
     * @return 处理的候选数
     */
    @Operation(summary = "刷新命中率追踪", description = "回看窗口内信号日前向收益重算（幂等）")
    @SysLog("刷新命中率追踪")
    @PostMapping("/refresh")
    public R<Integer> refresh() {
        return R.ok(stockCandidateHitService.refreshHits());
    }

    /**
     * 刷新命中率追踪（内部接口，供 Quartz/流水线 Feign 调用）
     * @return 处理的候选数
     */
    @Inner
    @Operation(summary = "刷新命中率追踪（内部）", description = "内部接口：供 Quartz/流水线调用")
    @SysLog("刷新命中率追踪(内部)")
    @PostMapping("/syncRefresh")
    public R<Integer> syncRefresh() {
        return R.ok(stockCandidateHitService.refreshHits());
    }

}
