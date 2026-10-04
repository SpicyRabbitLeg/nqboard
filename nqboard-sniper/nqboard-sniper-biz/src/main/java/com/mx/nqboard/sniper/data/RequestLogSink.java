package com.mx.nqboard.sniper.data;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.nqboard.sniper.api.entity.RequestLogEntity;
import com.mx.nqboard.sniper.service.RequestLogService;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 请求审计落表实现（Python CN_REQUEST_LOG 等价物，05 模块外部请求审计页的数据源）：
 * </p>
 * <ul>
 * <li>append-only：审计列由写入路径显式赋值（create_by='sniper'、del_flag='0'），不依赖填充器；
 * called_at 走 DB 默认 CURRENT_TIMESTAMP(3)</li>
 * <li>params 列为 MySQL json 类型：非 JSON 文本（如完整 URL）包装为 {@code {"raw": "…"}} 存入</li>
 * <li>Java 侧默认全量记录（Python CN_REQUEST_LOG 为 opt-in JSONL）；失败也落行（error 摘要 ≤500 字），
 * 这是 fail-closed 可审计与对拍 fixtures 录制的前提</li>
 * <li>审计写失败只告警不阻断主链路（05 文档 §4 语义）</li>
 * </ul>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Slf4j
public class RequestLogSink implements RequestAuditSink {

	/** error 列宽 varchar(500)，超长截断 */
	private static final int ERROR_MAX_LENGTH = 500;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final RequestLogService requestLogService;

	public RequestLogSink(RequestLogService requestLogService) {
		this.requestLogService = requestLogService;
	}

	@Override
	public void accept(String endpoint, String source, String paramsJson, String status, Integer nRows, Long elapsedMs,
			String error) {
		try {
			RequestLogEntity entity = new RequestLogEntity();
			// append-only 审计列显式赋值（主文档 §3 头注：不依赖填充器）
			entity.setCreateBy("sniper");
			LocalDateTime now = LocalDateTime.now();
			entity.setCreateTime(now);
			entity.setUpdateBy("sniper");
			entity.setUpdateTime(now);
			entity.setDelFlag("0");
			entity.setEndpoint(endpoint);
			entity.setSource(source);
			entity.setParams(toSafeJson(paramsJson));
			entity.setStatus(status != null ? status : "error");
			entity.setNRows(nRows);
			entity.setElapsedMs(elapsedMs == null ? null : elapsedMs.intValue());
			entity.setError(truncate(error));
			requestLogService.save(entity);
		}
		catch (RuntimeException e) {
			// 审计失败不阻断主链路，仅告警（05 文档 §4）
			log.warn("sniper_request_log write failed (ignored): endpoint={} error={}", endpoint, e.getMessage());
		}
	}

	/**
	 * params 列是 json 类型：合法 JSON 原样存；纯文本（如完整 URL）包装 {"raw": …}
	 */
	private String toSafeJson(String text) {
		if (text == null) {
			return null;
		}
		try {
			MAPPER.readTree(text);
			return text;
		}
		catch (java.io.IOException e) {
			try {
				return MAPPER.writeValueAsString(java.util.Map.of("raw", truncate(text)));
			}
			catch (java.io.IOException ignored) {
				return null;
			}
		}
	}

	private String truncate(String text) {
		if (text == null) {
			return null;
		}
		return text.length() <= ERROR_MAX_LENGTH ? text : text.substring(0, ERROR_MAX_LENGTH);
	}

}
