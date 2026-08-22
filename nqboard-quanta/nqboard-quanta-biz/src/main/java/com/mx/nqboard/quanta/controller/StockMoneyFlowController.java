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
import com.mx.nqboard.quanta.api.entity.StockMoneyFlowEntity;
import com.mx.nqboard.quanta.api.vo.StockMoneyFlowExportVO;
import com.mx.nqboard.quanta.service.StockMoneyFlowService;
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
 * 个股主力资金流 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockMoneyFlow")
@Tag(description = "stockMoneyFlow", name = "个股主力资金流模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockMoneyFlowController {

    private final StockMoneyFlowService stockMoneyFlowService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockMoneyFlow 个股主力资金流
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockMoneyFlowPage(@ParameterObject Page<StockMoneyFlowEntity> page,
                                   @ParameterObject StockMoneyFlowEntity stockMoneyFlow) {
        LambdaQueryWrapper<StockMoneyFlowEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockMoneyFlow.getTsCode()), StockMoneyFlowEntity::getTsCode, stockMoneyFlow.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockMoneyFlow.getTradeDate()), StockMoneyFlowEntity::getTradeDate, stockMoneyFlow.getTradeDate());
        wrapper.orderByDesc(StockMoneyFlowEntity::getTradeDate).orderByDesc(StockMoneyFlowEntity::getMainNetInflow);
        return R.ok(stockMoneyFlowService.page(page, wrapper));
    }

    /**
     * 通过条件查询个股主力资金流
     * @param stockMoneyFlow 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockMoneyFlowEntity stockMoneyFlow) {
        return R.ok(stockMoneyFlowService.list(Wrappers.query(stockMoneyFlow)));
    }

    /**
     * 新增个股主力资金流
     * @param stockMoneyFlow 个股主力资金流
     * @return R
     */
    @Operation(summary = "新增个股主力资金流", description = "新增个股主力资金流")
    @SysLog("新增个股主力资金流")
    @PostMapping
    @HasPermission("quanta_stockMoneyFlow_add")
    public R save(@Validated @RequestBody StockMoneyFlowEntity stockMoneyFlow) {
        return R.ok(stockMoneyFlowService.save(stockMoneyFlow));
    }

    /**
     * 修改个股主力资金流
     * @param stockMoneyFlow 个股主力资金流
     * @return R
     */
    @Operation(summary = "修改个股主力资金流", description = "修改个股主力资金流")
    @SysLog("修改个股主力资金流")
    @PutMapping
    @HasPermission("quanta_stockMoneyFlow_edit")
    public R updateById(@Validated @RequestBody StockMoneyFlowEntity stockMoneyFlow) {
        return R.ok(stockMoneyFlowService.updateById(stockMoneyFlow));
    }

    /**
     * 通过id删除个股主力资金流
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除个股主力资金流", description = "通过id删除个股主力资金流")
    @SysLog("通过id删除个股主力资金流")
    @DeleteMapping
    @HasPermission("quanta_stockMoneyFlow_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockMoneyFlowService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockMoneyFlow 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockMoneyFlow_export")
    public List<StockMoneyFlowExportVO> exportExcel(StockMoneyFlowEntity stockMoneyFlow, Long[] ids) {
        return BeanUtil.copyToList(
                stockMoneyFlowService.list(Wrappers.lambdaQuery(stockMoneyFlow)
                        .in(ArrayUtil.isNotEmpty(ids), StockMoneyFlowEntity::getId, ids)),
                StockMoneyFlowExportVO.class);
    }

    /**
     * 从 东方财富 同步个股主力资金流（当日全市场快照）
     * <p>
     * 系统内部接口：供 RemoteStockMoneyFlowService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
     * @return 同步成功的条数
     */
    @Inner
    @Operation(summary = "从 东方财富 同步数据", description = "同步个股主力资金流（当日全市场快照）")
    @SysLog("从 东方财富 同步个股主力资金流")
    @PostMapping("/sync")
    public R<Integer> syncFromEastMoney() {
        return R.ok(stockMoneyFlowService.syncFromEastMoney().getAffected());
    }

}
