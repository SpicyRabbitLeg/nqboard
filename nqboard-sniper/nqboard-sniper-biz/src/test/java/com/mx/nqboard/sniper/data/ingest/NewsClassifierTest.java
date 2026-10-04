package com.mx.nqboard.sniper.data.ingest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NewsClassifier 表驱动单测——关键词计数语义、五规则顺序、general 兜底（照 Python 原文）。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class NewsClassifierTest {

	@Test
	@DisplayName("sentiment：正计数>负计数→positive（计数按关键词命中个数）")
	void positiveWhenMorePositiveKeywords() {
		// 命中：利好、增长、增持（正 3）；减持（负 1）
		assertThat(NewsClassifier.sentiment("重大利好，业绩增长，股东增持，无减持压力", null)).isEqualTo("positive");
	}

	@Test
	@DisplayName("sentiment：负计数>正计数→negative")
	void negativeWhenMoreNegativeKeywords() {
		// 负：亏损、处罚、暴跌（3）；正：增长（1）
		assertThat(NewsClassifier.sentiment("业绩亏损，遭立案处罚，股价暴跌后难言增长", null)).isEqualTo("negative");
	}

	@Test
	@DisplayName("sentiment：正负相等→neutral；关键词重复出现只计一次")
	void neutralWhenBalancedAndDedupByKeyword() {
		// 正：利好（1）；负：利空（1）——"利好"重复出现不重复计数
		assertThat(NewsClassifier.sentiment("利好利好，其实也是利空", null)).isEqualTo("neutral");
	}

	@Test
	@DisplayName("sentiment：空文本→neutral")
	void neutralOnEmptyText() {
		assertThat(NewsClassifier.sentiment(null, null)).isEqualTo("neutral");
		assertThat(NewsClassifier.sentiment("", "")).isEqualTo("neutral");
	}

	@Test
	@DisplayName("announcementType：五规则按序匹配，业绩预告优先")
	void announcementRulesInOrder() {
		assertThat(NewsClassifier.announcementType("公司发布业绩预告", null)).isEqualTo("业绩预告");
		assertThat(NewsClassifier.announcementType("回购股份公告", null)).isEqualTo("回购");
		assertThat(NewsClassifier.announcementType("股东减持计划", null)).isEqualTo("减持");
		assertThat(NewsClassifier.announcementType("收到立案问询", null)).isEqualTo("监管");
		assertThat(NewsClassifier.announcementType("央行发布政策", null)).isEqualTo("政策");
		// 同文本含多类关键词：业绩预告规则在前优先
		assertThat(NewsClassifier.announcementType("业绩预告显示拟回购", null)).isEqualTo("业绩预告");
	}

	@Test
	@DisplayName("announcementType：无匹配兜底 general（源码语义），空文本 null")
	void generalFallbackAndNullOnEmpty() {
		assertThat(NewsClassifier.announcementType("今日召开股东大会", null)).isEqualTo("general");
		assertThat(NewsClassifier.announcementType(null, null)).isNull();
	}

}
