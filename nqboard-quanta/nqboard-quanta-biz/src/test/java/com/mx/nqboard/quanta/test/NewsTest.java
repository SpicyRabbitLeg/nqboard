package com.mx.nqboard.quanta.test;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsTest {

	public static class StockNews {
		private String stockCode;
		private String title;
		private String contentSummary;
		private String publishTime;
		private String source;
		private String url;

		public String getStockCode() {
			return stockCode;
		}

		public void setStockCode(String stockCode) {
			this.stockCode = stockCode;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContentSummary() {
			return contentSummary;
		}

		public void setContentSummary(String contentSummary) {
			this.contentSummary = contentSummary;
		}

		public String getPublishTime() {
			return publishTime;
		}

		public void setPublishTime(String publishTime) {
			this.publishTime = publishTime;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	public static List<StockNews> fetchStockNewsHttp(String stockCode, int pageIndex, int pageSize) {

		Map<String, Object> cmsParam = new HashMap<>();
		cmsParam.put("searchScope", "default");
		cmsParam.put("sort", "default");
		cmsParam.put("pageIndex", pageIndex);
		cmsParam.put("pageSize", pageSize);
		// 重点删除 preTag postTag！后端不支持这两个，会报非法JSON
//        cmsParam.put("preTag", "<em>");
//        cmsParam.put("postTag", "</em>");

		Map<String, Object> paramPayload = new HashMap<>();
		paramPayload.put("uid", "");
		paramPayload.put("keyword", stockCode);
		paramPayload.put("type", List.of("cmsArticleWebOld"));
		paramPayload.put("client", "web");
		paramPayload.put("clientType", "web");
		paramPayload.put("clientVersion", "curr");
		paramPayload.put("param", cmsParam);

		String paramJson = JSON.toJSONString(paramPayload,
				SerializerFeature.WriteNonStringKeyAsString,
				SerializerFeature.DisableCircularReferenceDetect);

		System.out.println("paramJson:" + paramJson);

		String encodeParam = URLEncodeUtil.encode(paramJson);
		String fullUrl = "https://search-api-web.eastmoney.com/search/jsonp?cb=cb&param=" + encodeParam;

		HttpResponse response = HttpRequest.get(fullUrl)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
				.header("Referer", "https://so.eastmoney.com/")
				.header("Accept", "*/*")
				.timeout(20000)
				.execute();

		int status = response.getStatus();
		String body = response.body();
		System.out.println("http status:" + status);
		System.out.println("response body:" + body);

		if (status != 200) {
			return new ArrayList<>();
		}

		String content = ReUtil.get("cb\\((.*)\\)", body, 1);
		if (StrUtil.isBlank(content)) {
			return new ArrayList<>();
		}

		JSONObject jsonObj;
		try {
			jsonObj = JSON.parseObject(content);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}

		JSONObject result = jsonObj.getJSONObject("result");
		if (result == null) {
			return new ArrayList<>();
		}
		JSONObject cmsArticle = result.getJSONObject("cmsArticleWebOld");
		if (cmsArticle == null) {
			return new ArrayList<>();
		}
		JSONArray list = cmsArticle.getJSONArray("list");
		if (list == null || list.isEmpty()) {
			return new ArrayList<>();
		}

		List<StockNews> outList = new ArrayList<>();
		for (Object temp : list) {
			JSONObject item = (JSONObject) temp;
			StockNews news = new StockNews();
			news.setStockCode(stockCode);
			// 不再有<em>标签，不需要替换
			String title = item.getString("title");
			String summary = item.getString("content");
			news.setTitle(title);
			news.setContentSummary(summary);
			news.setPublishTime(item.getString("showTime"));
			news.setSource(item.getString("media"));
			news.setUrl(item.getString("url"));
			outList.add(news);
		}
		return outList;
	}

	public static void main(String[] args) {
		List<StockNews> newsList = fetchStockNewsHttp("000831", 1, 100);
		System.out.println("拿到新闻数量：" + newsList.size());
		for (int i = 0; i < Math.min(3, newsList.size()); i++) {
			StockNews n = newsList.get(i);
			System.out.println(n.getPublishTime() + " | " + n.getTitle());
		}
	}
}
