package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockIndustryDailyEntity;
import com.mx.nqboard.quanta.mapper.StockIndustryDailyMapper;
import com.mx.nqboard.quanta.service.StockIndustryDailyService;
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
 * 行业板块日线K线 服务实现类
 * </p>
 * <p>
 * 数据来源：东财 clist 板块列表（fs=m:90+t:2 行业板块）+ push2his 板块K线（secid=90.BKxxxx）。
 * 板块数量约 86 个，请求间隔可由 yml 配置（industry-daily.request-interval-ms），
 * push2his CDN 对高频请求会临时封锁，默认间隔 3 秒（全量约 4~5 分钟）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndustryDailyServiceImpl extends ServiceImpl<StockIndustryDailyMapper, StockIndustryDailyEntity>
		implements StockIndustryDailyService {

	/**
	 * 东财 clist 板块列表接口
	 */
	private static final String CLIST_URL = "https://push2.eastmoney.com/api/qt/clist/get";

	/**
	 * 东财 push2his K线接口
	 */
	private static final String KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";

	/**
	 * 行业板块列表过滤条件
	 */
	private static final String FS_INDUSTRY_BOARD = "m:90+t:2+f:!50";

	/**
	 * 板块市场前缀（东财 secid 规则：90=板块）
	 */
	private static final String BOARD_SECID_PREFIX = "90.";

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * 单板块请求最大重试次数
	 */
	private static final int MAX_RETRY = 3;

	/**
	 * 重试基础退避间隔（毫秒），第 n 次重试等待 base * n
	 */
	private static final long RETRY_BASE_DELAY_MS = 3000L;

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final StockIndustryDailyMapper stockIndustryDailyMapper;

	/**
	 * 相邻板块请求间隔（毫秒），yml 配置 industry-daily.request-interval-ms
	 */
	@Value("${industry-daily.request-interval-ms:3000}")
	private long requestIntervalMs;

	/**
	 * 是否全量同步（yml 配置 industry-daily.full）
	 */
	@Value("${industry-daily.full:false}")
	private boolean syncFull;

	@Override
	public int syncFromEastMoney() {
		return syncFromEastMoney(null);
	}

	@Override
	public int syncFromEastMoney(Boolean full) {
		boolean syncFull = full != null ? full : this.syncFull;
		String beg = syncFull ? "0" : LocalDate.now().format(BASIC_DATE);

		List<Map<String, String>> boards = fetchBoardList();
		if (CollUtil.isEmpty(boards)) {
			throw new IllegalStateException("东方财富 板块列表接口无数据返回");
		}
		log.info("开始从 东方财富 同步行业板块日线, 板块数={}, full={}, beg={}", boards.size(), syncFull, beg);

		int total = 0;
		int failCount = 0;
		for (int i = 0; i < boards.size(); i++) {
			String boardCode = boards.get(i).get("code");
			String boardName = boards.get(i).get("name");
			try {
				List<StockIndustryDailyEntity> rows = fetchKline(boardCode, boardName, beg);
				total += upsertByUniqueKey(boardCode, rows);
			}
			catch (Exception e) {
				failCount++;
				log.error("同步板块 {}({}) 日线失败: {}", boardCode, boardName, e.getMessage());
			}
			if ((i + 1) % 20 == 0) {
				log.info("板块日线同步进度: {}/{}, 累计影响 {} 行, 失败 {} 个", i + 1, boards.size(), total, failCount);
			}
			if (requestIntervalMs > 0) {
				try {
					Thread.sleep(requestIntervalMs);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					log.warn("板块日线同步被中断");
					break;
				}
			}
		}
		log.info("从 东方财富 同步行业板块日线完成, 板块数={}, 累计影响 {} 行, 失败 {} 个", boards.size(), total, failCount);
		return total;
	}

	/**
	 * 拉取行业板块列表 [{code: BK0475, name: 银行}]
	 */
	private List<Map<String, String>> fetchBoardList() {
		Map<String, Object> params = Map.of(
				"pn", 1,
				"pz", 200,
				"po", 1,
				"np", 1,
				"fltt", 2,
				"invt", 2,
				"fid", "f3",
				"fs", FS_INDUSTRY_BOARD,
				"fields", "f12,f14");

		String respBody = HttpRequest.get(CLIST_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.timeout(15000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null || body.getJSONObject("data") == null) {
			return new ArrayList<>();
		}
		JSONArray diff = body.getJSONObject("data").getJSONArray("diff");
		List<Map<String, String>> boards = new ArrayList<>();
		if (diff != null) {
			for (int i = 0; i < diff.size(); i++) {
				JSONObject item = diff.getJSONObject(i);
				String code = item.getString("f12");
				String name = item.getString("f14");
				if (StrUtil.isNotBlank(code) && code.startsWith("BK")) {
					boards.add(Map.of("code", code, "name", StrUtil.nullToEmpty(name)));
				}
			}
		}
		return boards;
	}

	/**
	 * 调用东财 kline 接口拉取单个板块日线（失败自动退避重试）
	 * <p>
	 * klines 每行 CSV：日期,开盘,收盘,最高,最低,成交量,成交额（fields2=f51..f57）
	 */
	private List<StockIndustryDailyEntity> fetchKline(String boardCode, String boardName, String beg) {
		Exception last = null;
		for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
			try {
				return doFetchKline(boardCode, boardName, beg);
			}
			catch (Exception e) {
				last = e;
				log.warn("板块 {} kline 请求失败(第 {}/{} 次): {}", boardCode, attempt, MAX_RETRY, e.getMessage());
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
		throw new IllegalStateException("东方财富板块 kline 接口请求失败: " + (last != null ? last.getMessage() : "unknown"), last);
	}

	/**
	 * 单次调用东财 kline 接口
	 */
	private List<StockIndustryDailyEntity> doFetchKline(String boardCode, String boardName, String beg) {
		Map<String, Object> params = new HashMap<>(8);
		params.put("secid", BOARD_SECID_PREFIX + boardCode);
		params.put("fields1", "f1,f2,f3,f4,f5,f6");
		params.put("fields2", "f51,f52,f53,f54,f55,f56,f57");
		params.put("klt", 101);
		params.put("fqt", 0);
		params.put("beg", beg);
		params.put("end", "20990101");

		String respBody = HttpRequest.get(KLINE_URL)
				.form(params)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
				.timeout(15000)
				.execute()
				.body();
		JSONObject body = JSON.parseObject(respBody);
		if (body == null) {
			throw new IllegalStateException("东方财富板块 kline 接口响应为空");
		}
		JSONObject data = body.getJSONObject("data");
		if (data == null) {
			throw new IllegalStateException("东方财富板块 kline 接口无数据: " + body);
		}
		JSONArray klines = data.getJSONArray("klines");
		if (klines == null || klines.isEmpty()) {
			return new ArrayList<>();
		}
		List<StockIndustryDailyEntity> list = new ArrayList<>(klines.size());
		for (int i = 0; i < klines.size(); i++) {
			String line = klines.getString(i);
			if (StrUtil.isBlank(line)) {
				continue;
			}
			String[] cols = line.split(",");
			if (cols.length < 7) {
				continue;
			}
			StockIndustryDailyEntity entity = new StockIndustryDailyEntity();
			entity.setBoardCode(boardCode);
			entity.setBoardName(boardName);
			entity.setTradeDate(cols[0].trim());
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
	 * 按唯一键 (board_code, trade_date) 批量插入/更新
	 */
	private int upsertByUniqueKey(String boardCode, List<StockIndustryDailyEntity> rows) {
		if (CollUtil.isEmpty(rows)) {
			return 0;
		}
		List<StockIndustryDailyEntity> existList = list(Wrappers.<StockIndustryDailyEntity>lambdaQuery()
				.select(StockIndustryDailyEntity::getId, StockIndustryDailyEntity::getTradeDate)
				.eq(StockIndustryDailyEntity::getBoardCode, boardCode));
		Map<String, StockIndustryDailyEntity> existMap = existList.stream()
				.collect(Collectors.toMap(StockIndustryDailyEntity::getTradeDate, e -> e));

		List<StockIndustryDailyEntity> toInsert = new ArrayList<>();
		List<StockIndustryDailyEntity> toUpdate = new ArrayList<>();
		for (StockIndustryDailyEntity row : rows) {
			StockIndustryDailyEntity exist = existMap.get(row.getTradeDate());
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
