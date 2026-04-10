package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.device.api.dto.VideoAnalysisDTO;
import com.mx.nqboard.device.api.vo.VideoAnalysisResponseVO;
import com.mx.nqboard.device.service.VideoAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 摄像头AI服务实现类
 * @author 泥鳅压滑板
 */
@Slf4j
@Service
@RefreshScope
public class VideoAnalysisServiceImpl implements VideoAnalysisService {
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${video.analysis.base-url:http://127.0.0.1:9007}")
	private String baseUrl;

	private static final String REGISTER_URL = "/api/video/analysis/register";
	private static final String UPDATE_URL = "/api/video/analysis/update";
	private static final String DELETE_URL = "/api/video/analysis/delete";
	private static final String STATUS_URL = "/api/video/analysis/status";
	private static final String ONLINE_URL = "/api/video/analysis/status/%s";

	@Override
	public VideoAnalysisResponseVO register(VideoAnalysisDTO registerDTO) {
		log.info("注册摄像头AI服务，设备ID:{}", registerDTO.getDeviceId());
		return doRequest(REGISTER_URL, HttpMethod.POST, registerDTO);
	}

	@Override
	public VideoAnalysisResponseVO update(VideoAnalysisDTO updateDTO) {
		log.info("更新摄像头AI服务，设备ID:{}", updateDTO.getDeviceId());
		return doRequest(UPDATE_URL, HttpMethod.PUT, updateDTO);
	}

	@Override
	public VideoAnalysisResponseVO delete(VideoAnalysisDTO deleteDTO) {
		log.info("删除摄像头AI服务，设备ID:{}", deleteDTO.getDeviceId());
		return doRequest(DELETE_URL, HttpMethod.DELETE, deleteDTO);
	}

	@Override
	public VideoAnalysisResponseVO controlStatus(VideoAnalysisDTO statusDTO) {
		log.info("控制摄像头AI服务状态，设备ID:{}, 操作:{}", statusDTO.getDeviceId(), statusDTO.getOperate());
		return doRequest(STATUS_URL, HttpMethod.PUT, statusDTO);
	}

	@Override
	public VideoAnalysisResponseVO checkOnline(VideoAnalysisDTO deleteDTO) {
		log.info("查询摄像头AI服务，设备ID:{}", deleteDTO.getDeviceId());
		return doRequest(String.format(ONLINE_URL,deleteDTO.getDeviceId()), HttpMethod.GET, deleteDTO);
	}

	// ============================== 通用核心方法 ==============================

	/**
	 * 统一发送HTTP请求（抽离所有重复逻辑）
	 */
	private VideoAnalysisResponseVO doRequest(String path, HttpMethod method, VideoAnalysisDTO dto) {
		String url = baseUrl + path;
		try {
			HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(
					buildRequestBody(dto),
					buildJsonHeaders()
			);

			ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);
			return parseResponse(response);
		} catch (Exception e) {
			log.error("摄像头AI接口请求异常，URL:{}，设备ID:{}，错误:{}",
					url, dto.getDeviceId(), e.getMessage(), e);
			return buildErrorResponse("接口请求失败：" + e.getMessage());
		}
	}

	/**
	 * 构建JSON请求头
	 */
	private HttpHeaders buildJsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	/**
	 * 统一构建请求参数
	 */
	private Map<String, Object> buildRequestBody(VideoAnalysisDTO dto) {
		Map<String, Object> param = new HashMap<>(8);
		param.put("rtspUrl", dto.getRtspUrl());
		param.put("rtspUsername", StrUtil.nullToEmpty(dto.getRtspUsername()));
		param.put("rtspPassword", StrUtil.nullToEmpty(dto.getRtspPassword()));
		param.put("types", dto.getTypes());
		param.put("deviceId", dto.getDeviceId());
		param.put("callbackUrl", dto.getCallbackUrl());
		param.put("operate", dto.getOperate());
		return param;
	}

	/**
	 * 统一解析响应
	 */
	private VideoAnalysisResponseVO parseResponse(ResponseEntity<String> response) {
		if (response.getStatusCode() != HttpStatus.OK || StrUtil.isBlank(response.getBody())) {
			log.error("接口请求失败，状态码:{}", response.getStatusCode());
			return buildErrorResponse("请求失败，状态码：" + response.getStatusCode());
		}

		try {
			return JSON.parseObject(response.getBody(), VideoAnalysisResponseVO.class);
		} catch (Exception e) {
			log.error("响应解析失败:{}", response.getBody(), e);
			return buildErrorResponse("响应解析异常");
		}
	}

	/**
	 * 统一构建错误返回
	 */
	private VideoAnalysisResponseVO buildErrorResponse(String msg) {
		VideoAnalysisResponseVO vo = new VideoAnalysisResponseVO();
		vo.setCode(500);
		vo.setMsg(msg);
		vo.setData(StringConstants.Other.EMPTY);
		return vo;
	}
}