package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockConsWeightEntity;
import com.mx.nqboard.quanta.mapper.StockConsWeightMapper;
import com.mx.nqboard.quanta.service.StockConsWeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 指数成分股及权重 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockConsWeightServiceImpl extends ServiceImpl<StockConsWeightMapper, StockConsWeightEntity>
		implements StockConsWeightService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * 批量插入/更新每批条数
	 */
	private static final int BATCH_SIZE = 500;

	/**
	 * closeweight.xls 列索引（官网固定布局，首行为表头）
	 */
	private static final int COL_DATE = 0;
	private static final int COL_INDEX_CODE = 1;
	private static final int COL_INDEX_NAME = 2;
	private static final int COL_STOCK_CODE = 4;
	private static final int COL_EXCHANGE = 7;
	private static final int COL_WEIGHT = 9;

	private final StockConsWeightMapper stockConsWeightMapper;

	/**
	 * 指数代码列表（yml 配置 csindex.indexes，逗号分隔）
	 */
	@Value("${csindex.indexes:000300,000016,000905,000852,000500,000922}")
	private String indexes;

	/**
	 * 权重文件下载地址模板（yml 配置 csindex.url，{} 替换为指数代码）
	 */
	@Value("${csindex.url:https://oss-ch.csindex.com.cn/static/html/csindex/public/uploads/file/autofile/closeweight/{}closeweight.xls}")
	private String urlTemplate;

	/**
	 * 从 中证指数官网 同步指数成分股权重（下载 yml 配置的全部指数 closeweight.xls）
	 */
	@Override
	public int syncFromCsindex() {
		if (StrUtil.isBlank(indexes)) {
			throw new IllegalStateException("csindex.indexes 未配置，请在 yml 中配置待同步的指数代码列表");
		}
		String[] codes = indexes.split(",");
		int total = 0;
		int failCount = 0;
		for (String code : codes) {
			String indexCode = code.trim();
			if (StrUtil.isBlank(indexCode)) {
				continue;
			}
			String url = StrUtil.format(urlTemplate, indexCode);
			log.info("开始下载指数成分股权重: {}", url);
			try {
				byte[] bytes = download(url);
				int affected = upsertFile(bytes);
				total += affected;
				log.info("指数 {} 同步完成, 处理 {} 行", indexCode, affected);
			}
			catch (Exception e) {
				failCount++;
				log.error("同步指数 {} 失败: {}", indexCode, e.getMessage());
			}
		}
		log.info("指数成分股权重同步完成, 共处理 {} 行, 失败 {} 个指数", total, failCount);
		return total;
	}

	/**
	 * 从本地 xls 文件同步指数成分股权重
	 */
	@Override
	public int syncFromCsindex(String filePath) {
		File file = new File(filePath);
		if (!file.isFile()) {
			throw new IllegalStateException("文件不存在: " + filePath);
		}
		log.info("开始从本地文件同步指数成分股权重: {}", filePath);
		return upsertFile(FileUtil.readBytes(file));
	}

	/**
	 * 下载 xls 文件字节
	 */
	private byte[] download(String url) {
		HttpResponse resp = HttpRequest.get(url).timeout(30000).execute();
		if (!resp.isOk()) {
			throw new IllegalStateException("下载失败: HTTP " + resp.getStatus());
		}
		return resp.bodyBytes();
	}

	/**
	 * 解析 xls 并按唯一键 (ts_code, index_code, trade_date) 批量插入/更新
	 */
	private int upsertFile(byte[] bytes) {
		List<StockConsWeightEntity> rows = parseXls(bytes);
		if (CollUtil.isEmpty(rows)) {
			log.warn("xls 文件无有效数据行");
			return 0;
		}
		// 同一次调样的指数代码与生效日期一致，按唯一键查已有记录（仅取 id + ts_code）
		StockConsWeightEntity first = rows.get(0);
		List<StockConsWeightEntity> existList = list(Wrappers.<StockConsWeightEntity>lambdaQuery()
				.select(StockConsWeightEntity::getId, StockConsWeightEntity::getTsCode)
				.eq(StockConsWeightEntity::getIndexCode, first.getIndexCode())
				.eq(StockConsWeightEntity::getTradeDate, first.getTradeDate()));
		Map<String, Long> existMap = existList.stream()
				.collect(Collectors.toMap(StockConsWeightEntity::getTsCode, StockConsWeightEntity::getId));

		List<StockConsWeightEntity> toInsert = new ArrayList<>();
		List<StockConsWeightEntity> toUpdate = new ArrayList<>();
		for (StockConsWeightEntity row : rows) {
			Long id = existMap.get(row.getTsCode());
			if (id == null) {
				toInsert.add(row);
			}
			else {
				row.setId(id);
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
	 * 解析 closeweight.xls（Apache POI HSSF），跳过首行表头
	 * <p>
	 * 列布局：[0]日期(yyyyMMdd) [1]指数代码 [2]指数名称 [4]成份券代码 [7]交易所 [9]权重(%)
	 */
	private List<StockConsWeightEntity> parseXls(byte[] bytes) {
		List<StockConsWeightEntity> list = new ArrayList<>();
		try (HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheetAt(0);
			for (int r = 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				String dateStr = cellStr(row, COL_DATE);
				String indexCode = cellStr(row, COL_INDEX_CODE);
				String indexName = cellStr(row, COL_INDEX_NAME);
				String stockCode = cellStr(row, COL_STOCK_CODE);
				String exchange = cellStr(row, COL_EXCHANGE);
				String weightStr = cellStr(row, COL_WEIGHT);
				if (StrUtil.isBlank(dateStr) || StrUtil.isBlank(stockCode) || StrUtil.isBlank(indexCode)) {
					continue;
				}
				StockConsWeightEntity entity = new StockConsWeightEntity();
				// 官网成分券代码无后缀（如 000001），按交易所列补 .SH/.SZ
				entity.setTsCode(stockCode + suffixOf(exchange, stockCode));
				entity.setIndexCode(indexCode);
				entity.setIndexName(indexName);
				entity.setWeight(StrUtil.isBlank(weightStr) ? null : new BigDecimal(weightStr));
				entity.setTradeDate(parseDate(dateStr));
				list.add(entity);
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("解析 xls 失败: " + e.getMessage(), e);
		}
		return list;
	}

	/**
	 * 读取单元格字符串，null 转空串
	 */
	private String cellStr(Row row, int col) {
		if (row.getCell(col) == null) {
			return "";
		}
		return row.getCell(col).toString().trim();
	}

	/**
	 * 按交易所列补后缀，无法识别时按代码前缀兜底
	 */
	private String suffixOf(String exchange, String stockCode) {
		if (StrUtil.contains(exchange, "上海")) {
			return ".SH";
		}
		if (StrUtil.contains(exchange, "深圳")) {
			return ".SZ";
		}
		if (stockCode.startsWith("6") || stockCode.startsWith("9")) {
			return ".SH";
		}
		return ".SZ";
	}

	/**
	 * 解析日期：优先 yyyyMMdd（官网格式），兼容 ISO 格式
	 */
	private LocalDate parseDate(String value) {
		if (value.matches("\\d{8}")) {
			return LocalDate.parse(value, BASIC_DATE);
		}
		return LocalDate.parse(value);
	}

}
