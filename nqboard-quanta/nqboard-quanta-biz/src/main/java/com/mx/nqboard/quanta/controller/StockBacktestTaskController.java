package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.quanta.api.entity.StockBacktestTaskEntity;
import com.mx.nqboard.quanta.api.entity.StockBacktestTradeEntity;
import com.mx.nqboard.quanta.backtest.BacktestParams;
import com.mx.nqboard.quanta.service.StockBacktestTaskService;
import com.mx.nqboard.quanta.service.StockBacktestTradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 回测任务 前端控制器
 * </p>
 * <p>
 * 回测任务由系统异步执行，不提供手工新增/修改接口。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockBacktestTask")
@Tag(description = "stockBacktestTask", name = "回测任务模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockBacktestTaskController {

    private final StockBacktestTaskService stockBacktestTaskService;

    private final StockBacktestTradeService stockBacktestTradeService;

    /**
     * 创建回测任务并异步执行
     * @param params 回测参数（为空时取默认值：hs300_csi500 / 120日 / 65分 / 10万资金）
     * @return 任务id
     */
    @Operation(summary = "创建回测任务", description = "异步执行，进度轮询任务详情")
    @SysLog("创建回测任务")
    @PostMapping
    public R<Long> createTask(@RequestBody(required = false) BacktestParams params) {
        return R.ok(stockBacktestTaskService.createTask(params));
    }

    /**
     * 重跑指定任务
     * @param taskId 任务id
     * @return 任务id
     */
    @Operation(summary = "重跑任务", description = "删除旧成交明细后重新执行")
    @SysLog("重跑回测任务")
    @PostMapping("/rerun")
    public R<Long> rerun(@RequestParam("taskId") Long taskId) {
        return R.ok(stockBacktestTaskService.rerun(taskId));
    }

    /**
     * 分页查询任务列表
     * @param page 分页对象
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getTaskPage(@ParameterObject Page<StockBacktestTaskEntity> page) {
        return R.ok(stockBacktestTaskService.page(page,
                Wrappers.<StockBacktestTaskEntity>lambdaQuery()
                        .orderByDesc(StockBacktestTaskEntity::getId)));
    }

    /**
     * 任务详情（含 params/stats/equityCurve JSON）
     * @param taskId 任务id
     * @return 任务实体
     */
    @Operation(summary = "任务详情", description = "含统计结果与权益曲线")
    @GetMapping("/details")
    public R getDetails(@RequestParam("taskId") Long taskId) {
        return R.ok(stockBacktestTaskService.getById(taskId));
    }

    /**
     * 任务成交明细分页
     * @param page 分页对象
     * @param taskId 任务id
     * @return 分页对象
     */
    @Operation(summary = "成交明细", description = "任务成交明细分页")
    @GetMapping("/trades")
    public R getTrades(@ParameterObject Page<StockBacktestTradeEntity> page, @RequestParam("taskId") Long taskId) {
        return R.ok(stockBacktestTradeService.page(page,
                Wrappers.<StockBacktestTradeEntity>lambdaQuery()
                        .eq(StockBacktestTradeEntity::getTaskId, taskId)
                        .orderByAsc(StockBacktestTradeEntity::getEntryDate)));
    }

    /**
     * 任务统计结果（解析后的 stats JSON，便于前端直接渲染）
     * @param taskId 任务id
     * @return 统计 JSON 对象
     */
    @Operation(summary = "任务统计", description = "解析后的统计结果（胜率/盈亏比/回撤/离场分布/分桶校准）")
    @GetMapping("/stats")
    public R getStats(@RequestParam("taskId") Long taskId) {
        StockBacktestTaskEntity task = stockBacktestTaskService.getById(taskId);
        if (task == null || StrUtil.isBlank(task.getStats())) {
            return R.ok(null);
        }
        return R.ok(com.alibaba.fastjson.JSON.parse(task.getStats()));
    }

    /**
     * 任务权益曲线（解析后的 equityCurve JSON）
     * @param taskId 任务id
     * @return 权益曲线数组
     */
    @Operation(summary = "权益曲线", description = "解析后的权益曲线 [{date,equity}]")
    @GetMapping("/equityCurve")
    public R getEquityCurve(@RequestParam("taskId") Long taskId) {
        StockBacktestTaskEntity task = stockBacktestTaskService.getById(taskId);
        if (task == null || StrUtil.isBlank(task.getEquityCurve())) {
            return R.ok(null);
        }
        return R.ok(com.alibaba.fastjson.JSON.parse(task.getEquityCurve()));
    }

}
