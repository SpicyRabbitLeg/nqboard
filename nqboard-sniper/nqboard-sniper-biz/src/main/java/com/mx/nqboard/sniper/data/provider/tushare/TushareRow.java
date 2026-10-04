package com.mx.nqboard.sniper.data.provider.tushare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * Tushare 响应行访问器——按 {@code data.fields} 名取值（Tushare 信封约定：fields 是列名数组、
 * items 是行数组且与 fields 序一一对应，禁止假设列序固定，一律按名访问）。
 * </p>
 * <p>数值经 Jackson {@code USE_BIG_DECIMAL_FOR_FLOATS} 保精度（bit-exact 对拍红线），
 * 整数是 IntNode/LongNode、浮点统一 DecimalNode，{@link #dec(String)} 零精度损失。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public record TushareRow(java.util.Map<String, JsonNode> cells) {

	/**
	 * 取字符串值（null/缺失 → null；数字节点取其原文形式）
	 */
	public String str(String field) {
		JsonNode node = cells.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		String text = node.asText();
		return text.isEmpty() ? null : text;
	}

	/**
	 * 取数值（整数/浮点/字符串数字统一转 BigDecimal，精度无损）
	 */
	public BigDecimal dec(String field) {
		JsonNode node = cells.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isNumber()) {
			return node.decimalValue();
		}
		String text = node.asText();
		if (text.isEmpty() || "nan".equalsIgnoreCase(text) || "None".equals(text)) {
			return null;
		}
		return new BigDecimal(text.trim());
	}

	/**
	 * 取日期（兼容 YYYYMMDD 与 YYYY-MM-DD 两种来源格式）
	 */
	public LocalDate date(String field) {
		String text = str(field);
		if (text == null) {
			return null;
		}
		String compact = text.replace("-", "");
		if (compact.length() == 8 && compact.chars().allMatch(Character::isDigit)) {
			return LocalDate.parse(compact, DateTimeFormatter.BASIC_ISO_DATE);
		}
		return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
	}

}
