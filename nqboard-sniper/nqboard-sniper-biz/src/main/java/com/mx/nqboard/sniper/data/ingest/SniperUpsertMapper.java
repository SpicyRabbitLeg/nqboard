package com.mx.nqboard.sniper.data.ingest;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity;
import com.mx.nqboard.sniper.api.entity.IndustryBoardDailyEntity;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.api.entity.TradeCalendarEntity;

/**
 * <p>
 * sniper 行情/基础表批量 upsert（append-only 表专用，与各表通用 CRUD Mapper 分离）：
 * {@code INSERT ... ON DUPLICATE KEY UPDATE} 命中 UNIQUE KEY 即幂等重跑覆盖（主文档 §4.3.0 规则 2）；
 * 参数绑定（#{...}）无手拼 SQL；审计列由调用方经 {@link IngestAudit#fill} 显式赋值。
 * 业务列在命中 UK 时全部覆盖（qfq 重算覆盖依赖此语义），仅 id/create_by/create_time 不动。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Mapper
public interface SniperUpsertMapper {

	@Insert("""
			<script>
			INSERT INTO sniper_trade_calendar
			  (id, create_by, create_time, update_by, update_time, cal_date, is_open, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.calDate}, #{e.isOpen}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  is_open = new.is_open, update_time = new.update_time
			</script>""")
	int upsertTradeCalendar(@Param("list") List<TradeCalendarEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_stock_basic
			  (id, create_by, create_time, update_by, update_time, code, exchange, ts_code, name,
			   list_date, industry, industry_source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.exchange}, #{e.tsCode}, #{e.name},
			   #{e.listDate}, #{e.industry}, #{e.industrySource}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  exchange = new.exchange, ts_code = new.ts_code, name = new.name,
			  list_date = new.list_date, industry = new.industry, industry_source = new.industry_source,
			  update_time = new.update_time
			</script>""")
	int upsertStockBasic(@Param("list") List<StockBasicEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_index_constituents
			  (id, create_by, create_time, update_by, update_time, index_code, snapshot_date, code, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.indexCode}, #{e.snapshotDate}, #{e.code}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  update_time = new.update_time
			</script>""")
	int upsertIndexConstituents(@Param("list") List<IndexConstituentsEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_daily_price
			  (id, create_by, create_time, update_by, update_time, code, trade_date, adjust,
			   open, high, low, close, pre_close, volume, amount, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.tradeDate}, #{e.adjust},
			   #{e.open}, #{e.high}, #{e.low}, #{e.close}, #{e.preClose}, #{e.volume}, #{e.amount},
			   #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  open = new.open, high = new.high, low = new.low, close = new.close,
			  pre_close = new.pre_close, volume = new.volume, amount = new.amount,
			  source = new.source, update_time = new.update_time
			</script>""")
	int upsertDailyPrice(@Param("list") List<DailyPriceEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_adj_factor
			  (id, create_by, create_time, update_by, update_time, code, trade_date, factor, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.tradeDate}, #{e.factor}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  factor = new.factor, update_time = new.update_time
			</script>""")
	int upsertAdjFactor(@Param("list") List<AdjFactorEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_market_snapshot
			  (id, create_by, create_time, update_by, update_time, trade_date, code, name,
			   price, change_pct, open, high, low, prev_close, volume, amount, turnover_rate, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.tradeDate}, #{e.code}, #{e.name},
			   #{e.price}, #{e.changePct}, #{e.open}, #{e.high}, #{e.low}, #{e.prevClose},
			   #{e.volume}, #{e.amount}, #{e.turnoverRate}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  name = new.name, price = new.price, change_pct = new.change_pct,
			  open = new.open, high = new.high, low = new.low, prev_close = new.prev_close,
			  volume = new.volume, amount = new.amount, turnover_rate = new.turnover_rate,
			  source = new.source, update_time = new.update_time
			</script>""")
	int upsertMarketSnapshot(@Param("list") List<MarketSnapshotEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_industry_board_daily
			  (id, create_by, create_time, update_by, update_time, board_name, trade_date,
			   open, high, low, close, volume, amount, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.boardName}, #{e.tradeDate},
			   #{e.open}, #{e.high}, #{e.low}, #{e.close}, #{e.volume}, #{e.amount}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  open = new.open, high = new.high, low = new.low, close = new.close,
			  volume = new.volume, amount = new.amount, source = new.source, update_time = new.update_time
			</script>""")
	int upsertIndustryBoardDaily(@Param("list") List<IndustryBoardDailyEntity> list);

	// ------------------------------------------------------------------
	// 事件 6 表（T9；两腿财务各存一行 UK(code,report_period,source)；
	// 新闻 UK 含 title 前缀索引；减持 change_vol 存负数；金额单位见 DataIngestService/EventIngestService）
	// ------------------------------------------------------------------

	@Insert("""
			<script>
			INSERT INTO sniper_financial_indicator
			  (id, create_by, create_time, update_by, update_time, code, report_period,
			   gross_margin, net_margin, roe, revenue_growth, profit_growth,
			   pe_ttm, pb, ps_ttm, market_cap, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.reportPeriod},
			   #{e.grossMargin}, #{e.netMargin}, #{e.roe}, #{e.revenueGrowth}, #{e.profitGrowth},
			   #{e.peTtm}, #{e.pb}, #{e.psTtm}, #{e.marketCap}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  gross_margin = new.gross_margin, net_margin = new.net_margin, roe = new.roe,
			  revenue_growth = new.revenue_growth, profit_growth = new.profit_growth,
			  pe_ttm = new.pe_ttm, pb = new.pb, ps_ttm = new.ps_ttm, market_cap = new.market_cap,
			  update_time = new.update_time
			</script>""")
	int upsertFinancialIndicator(@Param("list") List<com.mx.nqboard.sniper.api.entity.FinancialIndicatorEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_company_news
			  (id, create_by, create_time, update_by, update_time, code, title, source_name, url,
			   body_digest, published_at, sentiment, announcement_type, fetch_date, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.title}, #{e.sourceName}, #{e.url},
			   #{e.bodyDigest}, #{e.publishedAt}, #{e.sentiment}, #{e.announcementType},
			   #{e.fetchDate}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  source_name = new.source_name, url = new.url, body_digest = new.body_digest,
			  sentiment = new.sentiment, announcement_type = new.announcement_type,
			  update_time = new.update_time
			</script>""")
	int upsertCompanyNews(@Param("list") List<com.mx.nqboard.sniper.api.entity.CompanyNewsEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_insider_trade
			  (id, create_by, create_time, update_by, update_time, code, ann_date, holder_name,
			   holder_type, change_vol, avg_price, after_shares, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.annDate}, #{e.holderName},
			   #{e.holderType}, #{e.changeVol}, #{e.avgPrice}, #{e.afterShares}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  holder_type = new.holder_type, change_vol = new.change_vol, avg_price = new.avg_price,
			  after_shares = new.after_shares, update_time = new.update_time
			</script>""")
	int upsertInsiderTrade(@Param("list") List<com.mx.nqboard.sniper.api.entity.InsiderTradeEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_fund_flow_daily
			  (id, create_by, create_time, update_by, update_time, code, trade_date,
			   main_net, super_net, large_net, medium_net, small_net, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.tradeDate},
			   #{e.mainNet}, #{e.superNet}, #{e.largeNet}, #{e.mediumNet}, #{e.smallNet},
			   #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  main_net = new.main_net, super_net = new.super_net, large_net = new.large_net,
			  medium_net = new.medium_net, small_net = new.small_net, update_time = new.update_time
			</script>""")
	int upsertFundFlowDaily(@Param("list") List<com.mx.nqboard.sniper.api.entity.FundFlowDailyEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_dragon_tiger
			  (id, create_by, create_time, update_by, update_time, code, trade_date, reason,
			   net_buy, buy_amt, sell_amt, change_pct, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.tradeDate}, #{e.reason},
			   #{e.netBuy}, #{e.buyAmt}, #{e.sellAmt}, #{e.changePct}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  net_buy = new.net_buy, buy_amt = new.buy_amt, sell_amt = new.sell_amt,
			  change_pct = new.change_pct, update_time = new.update_time
			</script>""")
	int upsertDragonTiger(@Param("list") List<com.mx.nqboard.sniper.api.entity.DragonTigerEntity> list);

	@Insert("""
			<script>
			INSERT INTO sniper_restricted_release
			  (id, create_by, create_time, update_by, update_time, code, plan_date, shares,
			   market_value, float_ratio, float_mv_ratio, source, fetched_at)
			VALUES
			<foreach collection="list" item="e" separator=",">
			  (#{e.id}, #{e.createBy}, #{e.createTime}, #{e.updateBy}, #{e.updateTime},
			   #{e.code}, #{e.planDate}, #{e.shares},
			   #{e.marketValue}, #{e.floatRatio}, #{e.floatMvRatio}, #{e.source}, #{e.fetchedAt})
			</foreach>
			AS new
			ON DUPLICATE KEY UPDATE
			  market_value = new.market_value, float_ratio = new.float_ratio,
			  float_mv_ratio = new.float_mv_ratio, update_time = new.update_time
			</script>""")
	int upsertRestrictedRelease(
			@Param("list") List<com.mx.nqboard.sniper.api.entity.RestrictedReleaseEntity> list);

	// ------------------------------------------------------------------
	// qfq 合成辅助查询（复权因子单调不减 → UK(code,trade_date) 反向扫第一行即全历史 max，O(1) 点查）
	// ------------------------------------------------------------------

	@Select("SELECT factor FROM sniper_adj_factor WHERE code = #{code} ORDER BY trade_date DESC LIMIT 1")
	java.math.BigDecimal selectMaxFactor(@Param("code") String code);

	@Select("""
			SELECT id, create_by, create_time, update_by, update_time, del_flag, code, trade_date, adjust,
			       open, high, low, close, pre_close, volume, amount, source, fetched_at
			  FROM sniper_daily_price WHERE trade_date = #{date} AND adjust = 'none'""")
	List<DailyPriceEntity> selectNoneBarsByDate(@Param("date") java.time.LocalDate date);

	@Select("""
			SELECT id, create_by, create_time, update_by, update_time, del_flag, code, trade_date, factor, fetched_at
			  FROM sniper_adj_factor WHERE trade_date = #{date}""")
	List<AdjFactorEntity> selectFactorsByDate(@Param("date") java.time.LocalDate date);

	@Select("""
			SELECT id, create_by, create_time, update_by, update_time, code, trade_date, adjust,
			       open, high, low, close, pre_close, volume, amount, source, fetched_at
			  FROM sniper_daily_price WHERE code = #{code} AND adjust = 'none' ORDER BY trade_date""")
	List<DailyPriceEntity> selectNoneBars(@Param("code") String code);

	@Select("""
			SELECT id, create_by, create_time, update_by, update_time, code, trade_date, factor, fetched_at
			  FROM sniper_adj_factor WHERE code = #{code} ORDER BY trade_date""")
	List<AdjFactorEntity> selectFactors(@Param("code") String code);

}
