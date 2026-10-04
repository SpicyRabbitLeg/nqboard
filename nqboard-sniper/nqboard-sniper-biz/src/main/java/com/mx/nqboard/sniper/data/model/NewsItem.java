package com.mx.nqboard.sniper.data.model;

/**
 * <p>
 * 个股新闻统一模型（对齐 Python CompanyNews 消费面：search-api-web jsonp 主源与兜底同源）。
 * title/content 已去除 EM {@code <em>} 高亮标签。
 * </p>
 *
 * @param title 新闻标题（去标签）
 * @param date 发布日期 YYYY-MM-DD（取原文前10位）
 * @param url 新闻链接
 * @param sourceName 文章来源（空默认"东方财富"）
 * @param content 正文（去标签，入库层截断 2000 字）
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public record NewsItem(String title, String date, String url, String sourceName, String content) {
}
