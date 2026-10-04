package com.mx.nqboard.sniper.data.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mx.nqboard.sniper.api.entity.AdjFactorEntity;
import com.mx.nqboard.sniper.api.entity.DailyPriceEntity;
import com.mx.nqboard.sniper.api.entity.IndexConstituentsEntity;
import com.mx.nqboard.sniper.api.entity.IndustryBoardDailyEntity;
import com.mx.nqboard.sniper.api.entity.MarketSnapshotEntity;
import com.mx.nqboard.sniper.api.entity.StockBasicEntity;
import com.mx.nqboard.sniper.api.entity.TradeCalendarEntity;
import com.mx.nqboard.sniper.api.enums.AdjustEnum;
import com.mx.nqboard.sniper.data.model.KlineBar;
import com.mx.nqboard.sniper.data.model.QuoteSnapshot;
import com.mx.nqboard.sniper.data.provider.CompositeProvider;
import com.mx.nqboard.sniper.data.provider.tushare.TushareClient;
import com.mx.nqboard.sniper.data.provider.tushare.TushareRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 行情/基础数据入库编排（主文档 §4.3 逐表入库规格 + §4.4 日更五阶段的 Service 层；
 * Quartz Job 与手动触发（T11）都调用本类，方法幂等可重跑——upsert 命中 UK 覆盖）：
 * </p>
 * <ul>
 * <li>单位纪律（§4.3.0 权威矩阵）：行情表原样入库 + source 标注（tushare amount=千元原样）；
 * 审计列经 {@link IngestAudit#fill} 显式赋值</li>
 * <li>qfq 合成：qfq = none × factor ÷ max_factor（该票全历史 max，含当日）——因子单调不减，
 * max 用 UK 反向点查 O(1)；除权票全历史重算用<b>库内 none+因子重算</b>（与重拉等价，
 * 等价性由 T12 抽样验证门把关）</li>
 * <li>失败语义：主源失败→降级→全链失败<b>记缺数标记返回</b>（不抛异常终止日更序列）</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestService {

	private static final DateTimeFormatter COMPACT = DateTimeFormatter.BASIC_ISO_DATE;

	/** upsert 分批大小（防超长 SQL） */
	private static final int BATCH = 1000;

	private final TushareClient tushareClient;

	private final CompositeProvider compositeProvider;

	private final SniperUpsertMapper upsertMapper;

	// ------------------------------------------------------------------
	// 交易日历（§4.3.1：trade_cal 全量约 9k 行 1 调；cal_date/is_open 原样）
	// ------------------------------------------------------------------

	public int ingestTradeCalendar(String startDate, String endDate) {
		List<TushareRow> rows = tushareClient.tradeCal(startDate, endDate);
		List<TradeCalendarEntity> entities = rows.stream().map(row -> {
			TradeCalendarEntity e = new TradeCalendarEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setCalDate(row.date("cal_date"));
			e.setIsOpen(row.dec("is_open") == null ? 0 : row.dec("is_open").intValue());
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).toList();
		return batchUpsert("trade_calendar", entities, upsertMapper::upsertTradeCalendar);
	}

	// ------------------------------------------------------------------
	// 股票基础（§4.3.2：ts_code 一拆三；f127 行业修正按需单独触发）
	// ------------------------------------------------------------------

	public int ingestStockBasic() {
		List<TushareRow> rows = tushareClient.stockBasic();
		List<StockBasicEntity> entities = rows.stream().map(row -> {
			StockBasicEntity e = new StockBasicEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			String tsCode = row.str("ts_code");
			if (tsCode == null || !tsCode.contains(".")) {
				return null;
			}
			String code = tsCode.substring(0, tsCode.indexOf('.'));
			e.setCode(code);
			e.setExchange(tsCode.substring(tsCode.indexOf('.') + 1));
			e.setTsCode(tsCode);
			e.setName(row.str("name"));
			e.setListDate(row.date("list_date"));
			e.setIndustry(row.str("industry"));
			e.setIndustrySource("tushare");
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).filter(java.util.Objects::nonNull).toList();
		return batchUpsert("stock_basic", entities, upsertMapper::upsertStockBasic);
	}

	// ------------------------------------------------------------------
	// 指数成分（§4.3.3：index_weight 跨多日快照 → 只取 max(trade_date) 全部行）
	// ------------------------------------------------------------------

	public int ingestIndexConstituents(String indexCode) {
		List<TushareRow> rows = tushareClient.indexWeight(indexCode);
		LocalDate latest = rows.stream()
			.map(r -> r.date("trade_date"))
			.filter(java.util.Objects::nonNull)
			.max(LocalDate::compareTo)
			.orElse(null);
		if (latest == null) {
			log.warn("index_weight empty for {}", indexCode);
			return 0;
		}
		List<IndexConstituentsEntity> entities = rows.stream()
			.filter(r -> latest.equals(r.date("trade_date")))
			.map(r -> {
				String con = r.str("con_code");
				// 先取"."前 6 位再正则校验（主文档 §4.3.3：con_code 取"."前 6 位，校验 6 位数字）
				String code = con != null && con.contains(".") ? con.substring(0, con.indexOf('.')) : con;
				if (code == null || !code.matches("\\d{6}")) {
					return null;
				}
				IndexConstituentsEntity e = new IndexConstituentsEntity();
				IngestAudit.fill(e);
				e.setId(IngestAudit.newId());
				e.setIndexCode(indexCode);
				e.setSnapshotDate(latest);
				e.setCode(code);
				e.setFetchedAt(java.time.LocalDateTime.now());
				return e;
			})
			.filter(java.util.Objects::nonNull)
			.toList();
		log.info("index constituents {}: snapshot {} -> {} codes", indexCode, latest, entities.size());
		return batchUpsert("index_constituents", entities, upsertMapper::upsertIndexConstituents);
	}

	// ------------------------------------------------------------------
	// 日行情 + 复权因子（§4.3.4 核心：批量日更 2 调 + 当日 qfq 合成）
	// ------------------------------------------------------------------

	/**
	 * 日更：daily(trade_date) 全市场 → none 行；adj_factor(trade_date) → 因子表；
	 * 随后按"含当日的全历史 max factor"合成当日 qfq 行（= {@link #ingestDailyBars} + {@link #synthQfqForDate}）。
	 * @return detail 标记（noneRows/factorRows/qfqRows 计数），供 sniper_daily_run.detail
	 */
	public Map<String, Object> ingestDailyAndFactors(LocalDate tradeDate) {
		Map<String, Object> detail = new LinkedHashMap<>(ingestDailyBars(tradeDate));
		detail.put("qfqRows", synthQfqForDate(tradeDate));
		return detail;
	}

	/**
	 * 仅 none 行 + 因子两段（<b>历史回填逐日调用</b>——回填场景 qfq 在全部 none/因子入库后
	 * 全市场一次性合成，逐日合成时库内 max_factor 仍在增长、中途 qfq 不正确）。
	 */
	public Map<String, Object> ingestDailyBars(LocalDate tradeDate) {
		String ymd = tradeDate.format(COMPACT);
		Map<String, Object> detail = new LinkedHashMap<>();

		List<DailyPriceEntity> noneBars = tushareClient.dailyByTradeDate(ymd).stream()
			.map(row -> toPriceEntity(row, tradeDate, AdjustEnum.NONE))
			.toList();
		detail.put("noneRows", batchUpsert("daily_price(none)", noneBars, upsertMapper::upsertDailyPrice));

		List<AdjFactorEntity> factors = tushareClient.adjFactorByTradeDate(ymd).stream().map(row -> {
			AdjFactorEntity e = new AdjFactorEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setCode(toCode(row.str("ts_code")));
			e.setTradeDate(tradeDate);
			e.setFactor(row.dec("adj_factor"));
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).toList();
		detail.put("factorRows", batchUpsert("adj_factor", factors, upsertMapper::upsertAdjFactor));
		return detail;
	}

	/**
	 * 当日 qfq 合成（日更 15:12）：查当日 none 行与当日因子，按各票全历史 max factor（点查）合成。
	 * @return qfq 行数
	 */
	public int synthQfqForDate(LocalDate tradeDate) {
		Map<String, DailyPriceEntity> noneByCode = new LinkedHashMap<>();
		for (DailyPriceEntity bar : upsertMapper.selectNoneBarsByDate(tradeDate)) {
			noneByCode.put(bar.getCode(), bar);
		}
		Map<String, BigDecimal> dailyFactor = new LinkedHashMap<>();
		for (AdjFactorEntity f : upsertMapper.selectFactorsByDate(tradeDate)) {
			dailyFactor.put(f.getCode(), f.getFactor());
		}
		List<DailyPriceEntity> qfqBars = new ArrayList<>(noneByCode.size());
		for (Map.Entry<String, DailyPriceEntity> entry : noneByCode.entrySet()) {
			BigDecimal factor = dailyFactor.get(entry.getKey());
			BigDecimal maxFactor = upsertMapper.selectMaxFactor(entry.getKey());
			if (factor == null || maxFactor == null || maxFactor.signum() <= 0) {
				continue;
			}
			qfqBars.add(synthQfqEntity(entry.getValue(), factor, maxFactor));
		}
		return batchUpsert("daily_price(qfq):" + tradeDate, qfqBars, upsertMapper::upsertDailyPrice);
	}

	/**
	 * 当日有因子行的全部票代码（回填 qfq 全市场合成的票集合来源）。
	 */
	public List<String> listCodesWithFactorsOn(LocalDate date) {
		return upsertMapper.selectFactorsByDate(date).stream()
			.map(AdjFactorEntity::getCode)
			.distinct()
			.toList();
	}

	/**
	 * 从指定日期的 none 行中随机抽样 code（qfq 抽样验证门的样本集）。
	 */
	public List<String> sampleCodes(int size, LocalDate asOf) {
		List<String> codes = new ArrayList<>(upsertMapper.selectNoneBarsByDate(asOf).stream()
			.map(DailyPriceEntity::getCode)
			.toList());
		java.util.Collections.shuffle(codes);
		return codes.subList(0, Math.min(Math.max(size, 1), codes.size()));
	}

	/**
	 * 指数日行情（index_daily → code=前6位、adjust='none'；市场门 5 日涨幅与台账基准基准消费）。
	 */
	public int ingestIndexDaily(String indexCode6, LocalDate start, LocalDate end) {
		String tsCode = indexCode6 + (indexCode6.startsWith("399") ? ".SZ" : ".SH");
		List<DailyPriceEntity> bars = tushareClient
			.indexDaily(tsCode, start.format(COMPACT), end.format(COMPACT))
			.stream()
			.map(row -> toPriceEntity(row, null, AdjustEnum.NONE))
			.map(e -> {
				e.setCode(indexCode6);
				return e;
			})
			.toList();
		return batchUpsert("index_daily:" + indexCode6, bars, upsertMapper::upsertDailyPrice);
	}

	/**
	 * 逐票缺口补拉（4 级链，CompositeProvider.prices）→ 按请求口径 upsert。
	 */
	public int fillPriceGap(String code, String exchange, LocalDate start, LocalDate end, AdjustEnum adjust) {
		List<KlineBar> bars = compositeProvider.prices(code, exchange, start, end, adjust,
				c -> upsertMapper.selectMaxFactor(c));
		List<DailyPriceEntity> entities = bars.stream().map(bar -> {
			DailyPriceEntity e = new DailyPriceEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setCode(code);
			e.setTradeDate(bar.tradeDate());
			e.setAdjust(adjust);
			e.setOpen(bar.open());
			e.setHigh(bar.high());
			e.setLow(bar.low());
			e.setClose(bar.close());
			e.setPreClose(bar.preClose());
			e.setVolume(bar.volume());
			e.setAmount(bar.amount());
			e.setSource(bar.source());
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).toList();
		return batchUpsert("price_gap:" + code + ":" + adjust, entities, upsertMapper::upsertDailyPrice);
	}

	/**
	 * 除权票全历史 qfq 重算（库内 none+因子重算，覆盖 upsert——与重拉等价，T12 验证门把关）。
	 * @return 重算 qfq 行数
	 */
	public int recalcQfqFull(String code) {
		List<DailyPriceEntity> noneBars = upsertMapper.selectNoneBars(code);
		List<AdjFactorEntity> factors = upsertMapper.selectFactors(code);
		if (noneBars.isEmpty() || factors.size() != noneBars.size()) {
			// 因子行应与 none 行一一对应；缺口时按 trade_date 对齐（缺失日期跳过）
			log.warn("recalcQfqFull {}: none={} factors={} (date-aligned join)", code, noneBars.size(),
					factors.size());
		}
		Map<LocalDate, BigDecimal> factorByDate = new LinkedHashMap<>();
		for (AdjFactorEntity f : factors) {
			factorByDate.put(f.getTradeDate(), f.getFactor());
		}
		// 全历史 max factor（内存一次算出）
		BigDecimal maxFactor = factors.stream()
			.map(AdjFactorEntity::getFactor)
			.filter(java.util.Objects::nonNull)
			.max(BigDecimal::compareTo)
			.orElse(null);
		if (maxFactor == null || maxFactor.signum() <= 0) {
			return 0;
		}
		List<DailyPriceEntity> qfqBars = new ArrayList<>(noneBars.size());
		for (DailyPriceEntity bar : noneBars) {
			BigDecimal factor = factorByDate.get(bar.getTradeDate());
			if (factor == null) {
				continue;
			}
			qfqBars.add(synthQfqEntity(bar, factor, maxFactor));
		}
		return batchUpsert("qfq-recalc:" + code, qfqBars, upsertMapper::upsertDailyPrice);
	}

	// ------------------------------------------------------------------
	// 全市场快照（§4.3.5：CompositeProvider.spot 3 级链 → 统一 schema 原样入库）
	// ------------------------------------------------------------------

	public int ingestSpot(LocalDate tradeDate, List<String> tencentSymbols) {
		List<QuoteSnapshot> snapshots = compositeProvider.spot(tencentSymbols);
		List<MarketSnapshotEntity> entities = snapshots.stream().map(s -> {
			MarketSnapshotEntity e = new MarketSnapshotEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setTradeDate(tradeDate);
			e.setCode(s.code());
			e.setName(s.name());
			e.setPrice(s.price());
			e.setChangePct(s.changePct());
			e.setOpen(s.open());
			e.setHigh(s.high());
			e.setLow(s.low());
			e.setPrevClose(s.prevClose());
			e.setVolume(s.volume());
			e.setAmount(s.amount());
			e.setTurnoverRate(s.turnoverRate());
			e.setSource(s.source());
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).toList();
		return batchUpsert("market_snapshot:" + tradeDate, entities, upsertMapper::upsertMarketSnapshot);
	}

	// ------------------------------------------------------------------
	// 行业板块日K（§4.3.6：deep 票按需拉近 120 日，EM 板块K线 O-C-H-L）
	// ------------------------------------------------------------------

	public int ingestBoardDaily(String boardName, LocalDate start, LocalDate end) {
		List<KlineBar> bars = compositeProvider.boardKline(boardName, start, end);
		List<IndustryBoardDailyEntity> entities = bars.stream().map(bar -> {
			IndustryBoardDailyEntity e = new IndustryBoardDailyEntity();
			IngestAudit.fill(e);
			e.setId(IngestAudit.newId());
			e.setBoardName(boardName);
			e.setTradeDate(bar.tradeDate());
			e.setOpen(bar.open());
			e.setHigh(bar.high());
			e.setLow(bar.low());
			e.setClose(bar.close());
			e.setVolume(bar.volume());
			e.setAmount(bar.amount());
			e.setSource(bar.source());
			e.setFetchedAt(java.time.LocalDateTime.now());
			return e;
		}).toList();
		return batchUpsert("board_daily:" + boardName, entities, upsertMapper::upsertIndustryBoardDaily);
	}

	// ------------------------------------------------------------------
	// 私有段
	// ------------------------------------------------------------------

	private DailyPriceEntity toPriceEntity(TushareRow row, LocalDate tradeDate, AdjustEnum adjust) {
		DailyPriceEntity e = new DailyPriceEntity();
		IngestAudit.fill(e);
		e.setId(IngestAudit.newId());
		e.setCode(toCode(row.str("ts_code")));
		e.setTradeDate(tradeDate != null ? tradeDate : row.date("trade_date"));
		e.setAdjust(adjust);
		e.setOpen(row.dec("open"));
		e.setHigh(row.dec("high"));
		e.setLow(row.dec("low"));
		e.setClose(row.dec("close"));
		e.setPreClose(row.dec("pre_close"));
		// 单位原样：vol=手、amount=千元（tushare daily；bit-exact 优先，消费侧按 source 换算）
		e.setVolume(row.dec("vol"));
		e.setAmount(row.dec("amount"));
		e.setSource("tushare");
		e.setFetchedAt(java.time.LocalDateTime.now());
		return e;
	}

	/** qfq 合成：OHLC 四列同乘 factor÷max_factor；volume/amount/pre_close 不乘（复权只作用价格） */
	private static DailyPriceEntity synthQfqEntity(DailyPriceEntity none, BigDecimal factor, BigDecimal maxFactor) {
		DailyPriceEntity e = new DailyPriceEntity();
		IngestAudit.fill(e);
		e.setId(IngestAudit.newId());
		e.setCode(none.getCode());
		e.setTradeDate(none.getTradeDate());
		e.setAdjust(AdjustEnum.QFQ);
		e.setOpen(mul(none.getOpen(), factor, maxFactor));
		e.setHigh(mul(none.getHigh(), factor, maxFactor));
		e.setLow(mul(none.getLow(), factor, maxFactor));
		e.setClose(mul(none.getClose(), factor, maxFactor));
		e.setPreClose(none.getPreClose());
		e.setVolume(none.getVolume());
		e.setAmount(none.getAmount());
		e.setSource(none.getSource());
		e.setFetchedAt(java.time.LocalDateTime.now());
		return e;
	}

	private static BigDecimal mul(BigDecimal price, BigDecimal factor, BigDecimal maxFactor) {
		if (price == null || factor == null) {
			return null;
		}
		// scale=4 对齐列 decimal(16,4)；除法中间不截断（先乘后除）
		return price.multiply(factor).divide(maxFactor, 4, RoundingMode.HALF_UP);
	}

	private static String toCode(String tsCode) {
		if (tsCode == null) {
			return null;
		}
		int dot = tsCode.indexOf('.');
		return dot > 0 ? tsCode.substring(0, dot) : tsCode;
	}

	private <E> int batchUpsert(String label, List<E> entities, java.util.function.ToIntFunction<List<E>> upsert) {
		if (entities.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (int i = 0; i < entities.size(); i += BATCH) {
			List<E> batch = entities.subList(i, Math.min(i + BATCH, entities.size()));
			total += upsert.applyAsInt(batch);
		}
		log.info("ingest {}: {} rows (upsert {})", label, entities.size(), total);
		return total;
	}

}
