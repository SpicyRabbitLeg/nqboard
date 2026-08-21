package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockMotAnnNewsEntity;

/**
 * <p>
 * 公告&媒体新闻表 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockMotAnnNewsService extends IService<StockMotAnnNewsEntity> {

	/**
	 * 按股票代码同步公告&媒体新闻（全量/增量取 yml 配置 tushare.daily.full）
	 * <p>
	 * 数据来源：东方财富（个股公告 + 媒体新闻），东方财富连续失败 N 次后自动降级到巨潮资讯（公告）。
	 * 本接口为单股票手动/外部调用入口，不做全市场定时同步（防 IP 被封）
	 * @param tsCode 股票代码，如 002594.SZ
	 * @return 同步成功的条数
	 */
	int syncNews(String tsCode);

	/**
	 * 按股票代码同步公告&媒体新闻
	 * @param tsCode 股票代码，如 002594.SZ
	 * @param full 是否全量同步：true=2026-08-01 至今天；false=仅今天；为空取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	int syncNews(String tsCode, Boolean full);

}
