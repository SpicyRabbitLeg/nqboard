package com.mx.nqboard.sniper.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.CompanyNewsEntity;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.api.entity.DragonTigerEntity;
import com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity;
import com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity;
import com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity;
import com.mx.nqboard.sniper.api.entity.InsiderTradeEntity;
import com.mx.nqboard.sniper.api.entity.IndustryBoardDailyEntity;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.SecurityStatus;

/**
 * <p>
 * 数据网关——短线管线消费行情/事件数据的<b>唯一 IO 边界</b>（主文档 §4.1；禁止绕过直查表）。
 * 读序（D2）：MySQL → 缺口拉取（HTTP）→ 回写 → 返回；Redis 热点缓存后置优化。
 * </p>
 * <p>纪律：<b>每个读方法显式收 asOf/区间</b>，窗口计算一律锚定入参回溯（消灭 Python 版
 * datetime.now() 前视偏差；唯一例外是 Stage0 次新过滤以快照日为锚）。
 * 接口返回类型直接用 api 实体与 data.model（不另建领域对象层，对主文档 §4.1 签名的实现性偏离）。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/04
 */
public interface DataGateway {

	/** 个股/指数通用日行情（库优先，缺口按 4 级链补拉回写；adjust 口径） */
	List<KlineBar> getPrices(String code, LocalDate start, LocalDate end, AdjustEnum adjust);

	/** 指数日行情（none 行；000300/000905/399006——市场门 5 日涨幅与台账基准消费） */
	List<KlineBar> getIndexPrices(String indexCode6, LocalDate start, LocalDate end);

	/** 全市场快照（Stage0 输入；库内当日无快照时走 3 级链拉取回写） */
	List<MarketSnapshotEntity> getSpotSnapshot(LocalDate asOf);

	/** 交易日序列（is_open=1；日历表空时退化为周一~周五近似，仅日志告警） */
	List<LocalDate> getTradingDays(LocalDate start, LocalDate end);

	/** 非交易日回退上一交易日 */
	LocalDate resolveAsOf(LocalDate raw);

	/** 指数成分（最新快照日；刷新由日更任务负责，本方法纯读） */
	List<IndexConstituentsEntity> getIndexConstituents(String indexCode);

	/** 股票基础（含 list_date/industry，缺该票返回 null） */
	StockBasicEntity getStockBasic(String code);

	/** 全市场复权因子（按交易日，除权检测/qfq 合成用） */
	Map<String, BigDecimal> getAdjFactors(LocalDate tradeDate);

	/** 财务指标两腿合并（腿1 报告期行+腿2 估值行各存一行按 source 区分；PIT 过滤在消费侧） */
	List<FinancialIndicatorEntity> getFinancialMetrics(String code, LocalDate asOf, int limit);

	/** 个股新闻（published_at≤asOf；库内不足 limit 条时按需拉取回写，0 条仅告警不拒票） */
	List<CompanyNewsEntity> getCompanyNews(String code, LocalDate asOf, LocalDate start, int limit);

	/** 股东增减持（ann_date≤asOf 最近 limit 条；库空按需拉取） */
	List<InsiderTradeEntity> getInsiderTrades(String code, LocalDate asOf, int limit);

	/** 主力资金流（最近 days 个交易日，单位元；库不足按需拉取） */
	List<FundFlowDailyEntity> getMainFundFlow(String code, LocalDate asOf, int days);

	/** 龙虎榜近 days 日（asOf 当日全市场 top_list 未拉时先拉） */
	List<DragonTigerEntity> getDragonTiger(String code, LocalDate asOf, int days);

	/** 限售解禁（全部历史+未来批次；查询 fail-open，缺失返回空列表） */
	List<RestrictedReleaseEntity> getRestrictedRelease(String code, LocalDate asOf);

	/** 行业分类（EM f127 口径优先；库内 tushare 旧口径时尝试 EM 修正回写） */
	String getStockIndustry(String code);

	/** 行业板块日K（asOf 回溯；不足时按需拉近 120 日回写） */
	List<IndustryBoardDailyEntity> getIndustryBoardHist(String boardName, LocalDate asOf);

	/** 停牌+不复权涨跌停价（唯一"必须不复权"路径；SecurityStatus 语义见其 javadoc） */
	SecurityStatus getSecurityStatus(String code, LocalDate asOf);

	/** 北向持股（港交所 2024-08 停发，恒返回空列表——M2 预检据此告警"北向资金数据不足"） */
	List<Object> getNorthboundHoldings(String code, LocalDate asOf);

}
