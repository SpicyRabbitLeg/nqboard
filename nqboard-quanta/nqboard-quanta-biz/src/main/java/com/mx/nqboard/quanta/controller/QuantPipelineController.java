package com.mx.nqboard.quanta.controller;

import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.log.annotation.SysLog;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.api.entity.QuantPipelineLogEntity;
import com.mx.nqboard.quanta.service.QuantPipelineService;
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
 * 盘后数据流水线 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/quantPipeline")
@Tag(description = "quantPipeline", name = "盘后数据流水线模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class QuantPipelineController {

    private final QuantPipelineService quantPipelineService;

    /**
     * 异步触发完整流水线（立即返回 runId，进度查看 /logs）
     * @return 本次运行的 runId
     */
    @Operation(summary = "异步触发完整流水线", description = "异步执行，立即返回运行id")
    @SysLog("触发盘后数据流水线")
    @PostMapping("/run")
    public R<String> runPipeline() {
        return R.ok(quantPipelineService.runPipelineAsync());
    }

    /**
     * 异步重跑单个步骤
     * @param step 步骤编码（见 /steps）
     * @return 本次运行的 runId
     */
    @Operation(summary = "异步重跑单个步骤", description = "单步重跑（失败断点重试）")
    @SysLog("重跑流水线步骤")
    @PostMapping("/runStep")
    public R<String> runStep(@RequestParam("step") String step) {
        return R.ok(quantPipelineService.runStepAsync(step));
    }

    /**
     * 查询流水线支持的全部步骤定义
     * @return [{step, stepName}]
     */
    @Operation(summary = "查询步骤定义", description = "流水线支持的全部步骤")
    @GetMapping("/steps")
    public R<List<Map<String, String>>> listSteps() {
        return R.ok(quantPipelineService.listSteps());
    }

    /**
     * 查询指定日期流水线各步骤最新执行状态（默认今天）
     * @param runDate 运行日期 YYYYMMDD，为空取今天
     * @return 步骤日志列表
     */
    @Operation(summary = "查询流水线执行状态", description = "指定日期各步骤最新执行状态")
    @GetMapping("/logs")
    public R<List<QuantPipelineLogEntity>> latestRuns(
            @RequestParam(value = "runDate", required = false) String runDate) {
        return R.ok(quantPipelineService.latestRuns(runDate));
    }

    /**
     * 数据就绪检查（股票日线当日覆盖率）
     * @return {tradeDate, basicCount, dailyCount, coverage, ready}
     */
    @Operation(summary = "数据就绪检查", description = "股票日线当日覆盖率检查")
    @GetMapping("/readiness")
    public R<Map<String, Object>> checkReadiness() {
        return R.ok(quantPipelineService.checkReadiness());
    }

    /**
     * 同步执行完整流水线（阻塞直至完成，仅供 Quartz/Feign 内部调用）
     * <p>
     * 系统内部接口：供 RemoteQuantPipelineService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @return 本次运行的 runId
     */
    @Inner
    @Operation(summary = "同步执行完整流水线", description = "内部接口：阻塞执行，供 Quartz 调用")
    @SysLog("同步执行盘后数据流水线")
    @PostMapping("/syncRun")
    public R<String> syncRun() {
        return R.ok(quantPipelineService.runPipeline());
    }

}
