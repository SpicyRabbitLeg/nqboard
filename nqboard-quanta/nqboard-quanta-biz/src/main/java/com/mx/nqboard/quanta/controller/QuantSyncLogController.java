package com.mx.nqboard.quanta.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.quanta.api.entity.QuantSyncLogEntity;
import com.mx.nqboard.quanta.service.QuantSyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 数据同步执行日志 前端控制器
 * </p>
 * <p>
 * 供前端按时间追溯各 Quartz 数据同步任务的成功/失败条数、耗时与异常
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/syncLog")
@Tag(description = "syncLog", name = "数据同步执行日志模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class QuantSyncLogController {

    private final QuantSyncLogService quantSyncLogService;

    /**
     * 分页查询同步日志（按运行日期区间 + 任务类型过滤，倒序）
     * @param page 分页参数
     * @param syncType 任务编码过滤：stock_daily/index_daily/...，为空查全部
     * @param startDate 运行日期区间起 YYYYMMDD（含）
     * @param endDate 运行日期区间止 YYYYMMDD（含）
     * @return 同步日志分页
     */
    @Operation(summary = "分页查询同步日志", description = "按运行日期区间与任务类型过滤，按开始时间倒序")
    @GetMapping("/page")
    public R<Page<QuantSyncLogEntity>> page(@ParameterObject Page<QuantSyncLogEntity> page,
                                            @RequestParam(value = "syncType", required = false) String syncType,
                                            @RequestParam(value = "startDate", required = false) String startDate,
                                            @RequestParam(value = "endDate", required = false) String endDate) {
        return R.ok(quantSyncLogService.page(page, Wrappers.<QuantSyncLogEntity>lambdaQuery()
                .eq(StrUtil.isNotBlank(syncType), QuantSyncLogEntity::getSyncType, syncType)
                .ge(StrUtil.isNotBlank(startDate), QuantSyncLogEntity::getRunDate, startDate)
                .le(StrUtil.isNotBlank(endDate), QuantSyncLogEntity::getRunDate, endDate)
                .orderByDesc(QuantSyncLogEntity::getBeginTime)));
    }

    /**
     * 查询指定日期各同步任务最新执行状态（默认今天，看板用）
     * @param runDate 运行日期 YYYYMMDD，为空取今天
     * @return 各任务最新日志列表
     */
    @Operation(summary = "查询当日同步状态", description = "指定日期各同步任务最新执行状态")
    @GetMapping("/latest")
    public R<List<QuantSyncLogEntity>> latestRuns(
            @RequestParam(value = "runDate", required = false) String runDate) {
        return R.ok(quantSyncLogService.latestRuns(runDate));
    }

}
