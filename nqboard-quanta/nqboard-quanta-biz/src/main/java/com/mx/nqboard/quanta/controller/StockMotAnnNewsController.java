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
import com.mx.nqboard.quanta.api.entity.StockMotAnnNewsEntity;
import com.mx.nqboard.quanta.api.vo.StockMotAnnNewsExportVO;
import com.mx.nqboard.quanta.service.StockMotAnnNewsService;
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
 * 公告&媒体新闻表 前端控制器
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/stockMotAnnNews")
@Tag(description = "stockMotAnnNews", name = "公告&媒体新闻模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class StockMotAnnNewsController {

    private final StockMotAnnNewsService stockMotAnnNewsService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param stockMotAnnNews 公告&媒体新闻
     * @return 分页对象
     */
    @Operation(summary = "分页查询", description = "分页查询")
    @GetMapping("/page")
    public R getStockMotAnnNewsPage(@ParameterObject Page<StockMotAnnNewsEntity> page,
                                    @ParameterObject StockMotAnnNewsEntity stockMotAnnNews) {
        LambdaQueryWrapper<StockMotAnnNewsEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(stockMotAnnNews.getTsCode()), StockMotAnnNewsEntity::getTsCode, stockMotAnnNews.getTsCode());
        wrapper.eq(StrUtil.isNotBlank(stockMotAnnNews.getPubDate()), StockMotAnnNewsEntity::getPubDate, stockMotAnnNews.getPubDate());
        wrapper.eq(StrUtil.isNotBlank(stockMotAnnNews.getNewsType()), StockMotAnnNewsEntity::getNewsType, stockMotAnnNews.getNewsType());
        wrapper.like(StrUtil.isNotBlank(stockMotAnnNews.getTitle()), StockMotAnnNewsEntity::getTitle, stockMotAnnNews.getTitle());
        wrapper.orderByDesc(StockMotAnnNewsEntity::getPubDate);
        return R.ok(stockMotAnnNewsService.page(page, wrapper));
    }

    /**
     * 通过条件查询公告&媒体新闻
     * @param stockMotAnnNews 查询条件
     * @return R 对象列表
     */
    @Operation(summary = "通过条件查询", description = "通过条件查询对象")
    @GetMapping("/details")
    public R getDetails(@ParameterObject StockMotAnnNewsEntity stockMotAnnNews) {
        return R.ok(stockMotAnnNewsService.list(Wrappers.query(stockMotAnnNews)));
    }

    /**
     * 新增公告&媒体新闻
     * @param stockMotAnnNews 公告&媒体新闻
     * @return R
     */
    @Operation(summary = "新增公告&媒体新闻", description = "新增公告&媒体新闻")
    @SysLog("新增公告&媒体新闻")
    @PostMapping
    @HasPermission("quanta_stockMotAnnNews_add")
    public R save(@Validated @RequestBody StockMotAnnNewsEntity stockMotAnnNews) {
        return R.ok(stockMotAnnNewsService.save(stockMotAnnNews));
    }

    /**
     * 修改公告&媒体新闻
     * @param stockMotAnnNews 公告&媒体新闻
     * @return R
     */
    @Operation(summary = "修改公告&媒体新闻", description = "修改公告&媒体新闻")
    @SysLog("修改公告&媒体新闻")
    @PutMapping
    @HasPermission("quanta_stockMotAnnNews_edit")
    public R updateById(@Validated @RequestBody StockMotAnnNewsEntity stockMotAnnNews) {
        return R.ok(stockMotAnnNewsService.updateById(stockMotAnnNews));
    }

    /**
     * 通过id删除公告&媒体新闻
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除公告&媒体新闻", description = "通过id删除公告&媒体新闻")
    @SysLog("通过id删除公告&媒体新闻")
    @DeleteMapping
    @HasPermission("quanta_stockMotAnnNews_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(stockMotAnnNewsService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param stockMotAnnNews 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @Operation(summary = "导出excel 表格", description = "导出excel 表格")
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("quanta_stockMotAnnNews_export")
    public List<StockMotAnnNewsExportVO> exportExcel(StockMotAnnNewsEntity stockMotAnnNews, Long[] ids) {
        return BeanUtil.copyToList(
                stockMotAnnNewsService.list(Wrappers.lambdaQuery(stockMotAnnNews)
                        .in(ArrayUtil.isNotEmpty(ids), StockMotAnnNewsEntity::getId, ids)),
                StockMotAnnNewsExportVO.class);
    }

    /**
     * 按股票代码同步公告&媒体新闻（对外接口，单股票手动/外部调用）
     * <p>
     * 数据来源：东方财富（个股公告+媒体新闻），东方财富连续失败 3 次后自动降级到巨潮资讯（公告）。
     * 不做全市场定时同步（防 IP 被封），故不设 {@code @Inner}，走正常鉴权
     * @param tsCode 股票代码，如 002594.SZ
     * @param full 是否全量同步：true=2024-01-01 至今天；false=仅今天；为空取 yml 配置 tushare.daily.full
     * @return 同步成功的条数
     */
    @Operation(summary = "按股票代码同步公告&新闻", description = "东方财富失败自动降级巨潮资讯（单股票接口）")
    @SysLog("按股票代码同步公告&新闻")
    @PostMapping("/sync")
    public R<Integer> syncNews(@RequestParam("tsCode") String tsCode,
                               @RequestParam(value = "full", required = false) Boolean full) {
        return R.ok(stockMotAnnNewsService.syncNews(tsCode, full));
    }
}
