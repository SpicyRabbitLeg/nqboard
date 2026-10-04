package com.mx.nqboard.sniper.data.ingest;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 个股新闻入库预打标（照抄 Python akshare_client 关键词表与规则原文，入库时确定）：
 * </p>
 * <ul>
 * <li>sentiment：正/负关键词<b>计数</b>（pos&gt;neg→positive、neg&gt;pos→negative、否则 neutral；
 * 空文本→neutral）——注意是出现个数计数，非命中去重</li>
 * <li>announcement_type：五规则按序匹配（title+body 拼接），无匹配兜底 <b>"general"</b>（源码语义）；
 * 空文本→null</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public final class NewsClassifier {

	/** 正向关键词 14 个（照 _POSITIVE_KW 原文；文档"15 个"为笔误，附录 A #20 追记） */
	private static final List<String> POSITIVE_KW = List.of("利好", "增长", "突破", "增持", "回购", "中标", "获批",
			"创新高", "上调", "盈利", "超预期", "涨停", "政策支持", "补贴");

	/** 负向关键词 15 个（照 _NEGATIVE_KW 原文） */
	private static final List<String> NEGATIVE_KW = List.of("利空", "下滑", "亏损", "减持", "处罚", "立案", "退市",
			"警示", "下调", "暴跌", "跌停", "监管", "问询", "违规", "诉讼");

	private static final List<Map.Entry<String, List<String>>> ANNOUNCEMENT_RULES = List.of(
			Map.entry("业绩预告", List.of("业绩预告", "业绩快报")),
			Map.entry("回购", List.of("回购")),
			Map.entry("减持", List.of("减持", "清仓")),
			Map.entry("监管", List.of("立案", "问询", "处罚", "监管")),
			Map.entry("政策", List.of("政策", "国务院", "央行", "证监会")));

	private NewsClassifier() {
	}

	/**
	 * 情绪预打标（_sentiment_from_text 语义）
	 * @return positive / negative / neutral
	 */
	public static String sentiment(String title, String body) {
		String text = (title == null ? "" : title) + (body == null ? "" : body);
		if (text.isEmpty()) {
			return "neutral";
		}
		int pos = 0;
		for (String kw : POSITIVE_KW) {
			if (text.contains(kw)) {
				pos++;
			}
		}
		int neg = 0;
		for (String kw : NEGATIVE_KW) {
			if (text.contains(kw)) {
				neg++;
			}
		}
		if (pos > neg) {
			return "positive";
		}
		if (neg > pos) {
			return "negative";
		}
		return "neutral";
	}

	/**
	 * 公告类型五规则（_classify_announcement 语义，按序匹配）
	 * @return 业绩预告/回购/减持/监管/政策；无匹配 general；text 全空 null
	 */
	public static String announcementType(String title, String body) {
		String text = (title == null ? "" : title) + (body == null ? "" : body);
		if (text.isEmpty()) {
			return null;
		}
		for (Map.Entry<String, List<String>> rule : ANNOUNCEMENT_RULES) {
			for (String kw : rule.getValue()) {
				if (text.contains(kw)) {
					return rule.getKey();
				}
			}
		}
		return "general";
	}

}
