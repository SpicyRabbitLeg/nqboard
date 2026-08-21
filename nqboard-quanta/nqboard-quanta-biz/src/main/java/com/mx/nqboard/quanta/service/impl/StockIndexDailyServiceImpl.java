package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import com.mx.nqboard.quanta.service.StockIndexDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 指数日线K线表 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndexDailyServiceImpl extends ServiceImpl<StockIndexDailyMapper, StockIndexDailyEntity>
		implements StockIndexDailyService {

	/**
	 * 东方财富指数K线接口
	 */
	private static final String KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";

	/**
	 * 新浪财经指数K线接口（东财不可用时的降级数据源）
	 */
	private static final String SINA_KLINE_URL = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 相邻指数请求间隔（毫秒）
	 * <p>
	 * push2his CDN 曾对本机 IP 临时封锁（请求过频触发），且每次同步仅请求几个指数，
	 * 故间隔放宽到 5 分钟，降低触发风险（6 个指数全量约 30 分钟）
	 */
	private static final long REQUEST_INTERVAL_MS = 5 * 60 * 1000L;

	/**
	 * 单指数请求最大重试次数（push2his CDN 偶发断连/临时封锁，退避重试提高成功率）
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 增量同步自愈回看天数（日历日）：增量起点 = 表内最新交易日 - N 天，漏跑自动回补
	 */
	private static final int INCREMENTAL_SELF_HEAL_DAYS = 7;

	/**
	 * 重试基础退避间隔（毫秒），第 n 次重试等待 base * n
	 */
	private static final long RETRY_BASE_DELAY_MS = 2000L;

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockIndexDailyMapper stockIndexDailyMapper;

	/**
	 * 复用日线全量同步开关（yml 配置 tushare.daily.full）
	 */
	@Value("${tushare.daily.full:false}")
	private boolean syncFull;

	/**
	 * 待同步指数代码列表（yml 配置 index.daily.indexes，逗号分隔，带市场前缀，如 sh000300,sz399001）
	 */
	@Value("${index.daily.indexes:sh000300,sh000016,sh000905,sh000852,sh000500,sh000922}")
	private String indexes;

	/**
	 * 无参重载，便于 Quartz 定时任务在未配置参数时直接调用，取 yml 配置
	 * @return 同步成功的条数
	 */
	@Override
	public int syncFromEastMoney() {
		return syncFromEastMoney(null);
	}

	/**
	 * 从 东方财富 同步指数日线K线
	 * <p>
	 * 全量：beg=20260101 从 2026-01-01 起；增量：beg=今天，仅当天起
	 */
	@Override
	public int syncFromEastMoney(Boolean full) {
		if (StrUtil.isBlank(indexes)) {
			throw new IllegalStateException("index.daily.indexes 未配置，请在 yml 中配置待同步的指数代码列表");
		}
		boolean syncFull = full != null ? full : this.syncFull;
		// 增量起点自愈：表内最新交易日前推 N 天（单日漏跑自动回补）；表为空则退化为全量起点
		String beg;
		if (syncFull) {
			beg = LocalDate.of(2026, 1, 1).format(BASIC_DATE);
		}
		else {
			LocalDate latest = latestTradeDate();
			LocalDate start = latest != null ? latest.minusDays(INCREMENTAL_SELF_HEAL_DAYS) : LocalDate.of(2026, 1, 1);
			beg = start.format(BASIC_DATE);
		}
		log.info("开始从 东方财富 同步指数日线, full={}, beg={}", syncFull, beg);

		String[] codes = indexes.split(",");
		int total = 0;
		int failCount = 0;
		for (int i = 0; i < codes.length; i++) {
			String indexCode = codes[i].trim();
			if (StrUtil.isBlank(indexCode)) {
				continue;
			}
			try {
				List<StockIndexDailyEntity> rows = fetchKlineWithFallback(indexCode, beg);
				int affected = upsertByUniqueKey(indexCode, rows);
				total += affected;
				log.info("指数 {} 同步完成, 影响 {} 行 ({}/{})", indexCode, affected, i + 1, codes.length);
			}
			catch (Exception e) {
				failCount++;
				log.error("同步指数 {} 失败: {}", indexCode, e.getMessage());
			}
			if (REQUEST_INTERVAL_MS > 0) {
				try {
					Thread.sleep(REQUEST_INTERVAL_MS);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("指数日线同步被中断");
					break;
				}
			}
		}
		log.info("指数日线同步完成, 指数数={}, 累计影响 {} 行, 失败 {} 个", codes.length, total, failCount);
		return total;
	}

	/**
	 * 调用东财 kline 接口拉取单个指数日线（失败自动退避重试）
	 * <p>
	 * 返回 klines 每行 CSV：日期,开盘,收盘,最高,最低,成交量,成交额（fields2=f51..f57）
	 */
	private List<StockIndexDailyEntity> fetchKline(String indexCode, String beg) {
		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				return doFetchKline(indexCode, beg);
			}
			catch (Exception e) {
				last = e;
				log.warn("指数 {} kline 请求失败(第 {}/{} 次): {}", indexCode, attempt, MAX_RETRY, e.getMessage());
				if (attempt < MAX_RETRY) {
					try {
						Thread.sleep(RETRY_BASE_DELAY_MS * attempt);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		throw new IllegalStateException("东方财富 kline 接口请求失败（可能被临时限流/封锁 IP，建议稍后重试）: " + last.getMessage(), last);
	}

	/**
	 * 拉取指数K线：优先东财，失败后自动切换新浪财经
	 */
	private List<StockIndexDailyEntity> fetchKlineWithFallback(String indexCode, String beg) {
		try {
			List<StockIndexDailyEntity> rows = fetchKline(indexCode, beg);
			if (rows.isEmpty()) {
				log.warn("东财指数 {} K线为空，尝试新浪财经兜底", indexCode);
				return fetchSinaKline(indexCode, beg);
			}
			return rows;
		}
		catch (Exception e) {
			log.warn("东财指数 {} K线失败，切换到新浪财经兜底: {}", indexCode, e.getMessage());
			return fetchSinaKline(indexCode, beg);
		}
	}

	/**
	 * 新浪财经指数K线（降级数据源）
	 */
	private List<StockIndexDailyEntity> fetchSinaKline(String indexCode, String beg) {
		Map<String, Object> params = new HashMap<>(8);
		params.put("symbol", indexCode);
		params.put("scale", 240);
		params.put("ma", "no");
		params.put("datalen", 1023);

		String respBody = HttpRequest.get(SINA_KLINE_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://finance.sina.com.cn")
				.timeout(15000)
				.execute()
				.body();
		if (StrUtil.isBlank(respBody)) {
			throw new IllegalStateException("新浪财经指数K线接口响应为空");
		}
		JSONArray arr = JSON.parseArray(respBody);
		if (arr == null || arr.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockIndexDailyEntity> list = new ArrayList<>(arr.size());
		for (int i = 0; i < arr.size(); i++) {
			JSONObject item = arr.getJSONObject(i);
			String day = item.getString("day");
			if (StrUtil.isBlank(day)) {
				continue;
			}
			String dayBasic = day.replace("-", "");
			if (dayBasic.compareTo(beg) < 0) {
				continue;
			}
			StockIndexDailyEntity entity = new StockIndexDailyEntity();
			entity.setIndexCode(indexCode);
			entity.setTradeDate(dayBasic);
			entity.setOpen(decimal(item.getString("open")));
			entity.setClose(decimal(item.getString("close")));
			entity.setHigh(decimal(item.getString("high")));
			entity.setLow(decimal(item.getString("low")));
			entity.setVolume(decimal(item.getString("volume")));
			entity.setAmount(null);
			list.add(entity);
		}
		return list;
	}


	/**
	 * 单次调用东财 kline 接口
	 */
	private List<StockIndexDailyEntity> doFetchKline(String indexCode, String beg) {
		// sh → 沪市(市场1)，sz → 深市(市场0)
		String market = indexCode.startsWith("sz") ? "0" : "1";
		String code = indexCode.length() > 2 ? indexCode.substring(2) : indexCode;

		Map<String, Object> params = new HashMap<>(8);
		params.put("secid", market + "." + code);
		params.put("fields1", "f1,f2,f3,f4,f5,f6");
		params.put("fields2", "f51,f52,f53,f54,f55,f56,f57");
		params.put("klt", 101);
		params.put("fqt", 0);
		params.put("beg", beg);
		params.put("end", "20990101");

		String respBody = HttpRequest.get(KLINE_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.header("Referer", "https://quote.eastmoney.com/")
				.header("Accept", "*/*")
				.header("Accept-Language", "zh-CN,zh;q=0.9")
				.header("Connection", "close")
				.timeout(15000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null) {
			throw new IllegalStateException("东方财富 kline 接口响应为空");
		}
		JSONObject data = body.getJSONObject("data");
		if (data == null) {
			throw new IllegalStateException("东方财富 kline 接口无数据: " + body);
		}
		JSONArray klines = data.getJSONArray("klines");
		if (klines == null || klines.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockIndexDailyEntity> list = new ArrayList<>(klines.size());
		for (int i = 0; i < klines.size(); i++) {
			String line = klines.getString(i);
			if (StrUtil.isBlank(line)) {
				continue;
			}
			String[] cols = line.split(",");
			if (cols.length < 7) {
				log.debug("忽略异常K线行: {}", line);
				continue;
			}
			StockIndexDailyEntity entity = new StockIndexDailyEntity();
			entity.setIndexCode(indexCode);
			entity.setTradeDate(cols[0].trim().replace("-", ""));
			entity.setOpen(decimal(cols[1]));
			entity.setClose(decimal(cols[2]));
			entity.setHigh(decimal(cols[3]));
			entity.setLow(decimal(cols[4]));
			entity.setVolume(decimal(cols[5]));
			entity.setAmount(decimal(cols[6]));
			list.add(entity);
		}
		return list;
	}

	/**
	 * 表内最新交易日（自愈起点基准），空表返回 null
	 */
	private LocalDate latestTradeDate() {
		List<Object> dates = stockIndexDailyMapper.selectObjs(Wrappers.<StockIndexDailyEntity>query()
				.select("DISTINCT trade_date")
				.last("order by trade_date desc limit 1"));
		if (CollUtil.isEmpty(dates) || dates.get(0) == null) {
			return null;
		}
		String d = String.valueOf(dates.get(0)).replace("-", "");
		return d.length() == 8 ? LocalDate.parse(d, BASIC_DATE) : null;
	}

	/**
	 * 按唯一键 (index_code, trade_date) 批量插入/更新
	 */
	private int upsertByUniqueKey(String indexCode, List<StockIndexDailyEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		List<StockIndexDailyEntity> existList = list(Wrappers.<StockIndexDailyEntity>lambdaQuery()
				.select(StockIndexDailyEntity::getId, StockIndexDailyEntity::getTradeDate)
				.eq(StockIndexDailyEntity::getIndexCode, indexCode));
		Map<String, StockIndexDailyEntity> existMap = existList.stream()
				.collect(Collectors.toMap(StockIndexDailyEntity::getTradeDate, e -> e));

		List<StockIndexDailyEntity> toInsert = new ArrayList<>();
		List<StockIndexDailyEntity> toUpdate = new ArrayList<>();
		for (StockIndexDailyEntity row : rows) {
			StockIndexDailyEntity exist = existMap.get(row.getTradeDate());
			if (exist == null) {
				toInsert.add(row);
			}
			else {
				row.setId(exist.getId());
				toUpdate.add(row);
			}
		}
		if (!toInsert.isEmpty()) {
			saveBatch(toInsert, BATCH_SIZE);
		}
		if (!toUpdate.isEmpty()) {
			updateBatchById(toUpdate, BATCH_SIZE);
		}
		return rows.size();
	}

	/**
	 * 数值解析，空串返回 null
	 */
	private BigDecimal decimal(String value) {
		return StrUtil.isBlank(value) ? null : new BigDecimal(value.trim());
	}

}
