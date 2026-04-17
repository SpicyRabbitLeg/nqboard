package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.mx.nqboard.common.core.constant.DriverConstants;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.RedisUtils;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.VideoAnalysisDTO;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.VideoAnalysisResponseVO;
import com.mx.nqboard.device.service.DriverCustomService;
import com.mx.nqboard.device.service.IotDeviceService;
import com.mx.nqboard.device.service.IotProductService;
import com.mx.nqboard.device.service.VideoAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * rtsp视频推流服务
 *
 * @author SpicyRabbitLeg
 **/
@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class DriverCustomRtspServiceImpl implements DriverCustomService {

	private final IotDeviceService deviceService;

	private final IotProductService productService;

	private final VideoAnalysisService videoAnalysisService;

	@Value("${video.analysis.callback:http://127.0.0.1:9999/device/alarm-record/callback}")
	private String callbackUrl;

	@Override
	public String read(Map<String, Object> driverInfo, Map<String, Object> pointInfo) {
		// rtsp通过接口方式拉去视频流
		registerSource(getClient(driverInfo), pointInfo, driverInfo);
		return StringConstants.Other.EMPTY;
	}

	@Override
	public Boolean write(Map<String, Object> driverInfo, Map<String, Object> pointInfo, Object value) {
		return Boolean.TRUE;
	}

	@Override
	public String getStatus() {
		return DriverConstants.RTSP;
	}

	/**
	 * 获取Rtsp在线状态
	 *
	 * @param driverInfo 驱动信息
	 * @return MqttClient
	 */
	private synchronized String getClient(Map<String, Object> driverInfo) {
		log.debug("Rtsp client Server Connection Info {}", driverInfo);
		String serverUri = String.valueOf(driverInfo.get("serverUri"));

		String onlineStatus = RedisUtils.get(String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, serverUri));
		if (!StringConstants.Switch.ENABLE.equals(onlineStatus)) {
			try {
				new URI(serverUri);
				RedisUtils.set(String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, serverUri), StringConstants.Switch.ENABLE, 1, TimeUnit.MINUTES);
			} catch (Exception e) {
				log.error("get Rtsp status error: {}", e.getMessage());
				RedisUtils.delete(String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, serverUri));
				throw new RuntimeException(e.getMessage());
			}
		}
		return serverUri;
	}

	/**
	 * 启动视频监控流服务
	 * 调用内部服务进行视频分析【视频解析是cpu密集服务，需单独N台服务器操作】
	 *
	 * @param serverUri 服务地址
	 * @param pointInfo 位号属性配置
	 */
	private void registerSource(String serverUri, Map<String, Object> pointInfo, Map<String, Object> driverInfo) {
		String source = String.valueOf(pointInfo.get("source"));
		Long deviceId = NumberUtil.parseLong(String.valueOf(pointInfo.get("deviceId")));

		IotDeviceEntity deviceEntity = deviceService.getById(deviceId);
		Assert.isTrue(ObjectUtil.isNotEmpty(deviceEntity), "设备id错误，请重新校验 id: " + deviceId);

		IotProductEntity productEntity = productService.getById(deviceEntity.getProductId());
		Assert.isTrue(ObjectUtil.isNotEmpty(productEntity), "产品id错误，请重新校验 id: " + deviceEntity.getProductId());

		// 注册服务
		VideoAnalysisDTO videoAnalysisDto = new VideoAnalysisDTO();
		videoAnalysisDto.setRtspUrl(String.format("%s/%s", serverUri, source));
		videoAnalysisDto.setRtspPassword(String.valueOf(driverInfo.get("password")));
		videoAnalysisDto.setRtspUsername(String.valueOf(driverInfo.get("username")));
		videoAnalysisDto.setTypes(StrUtil.isNotBlank(productEntity.getCameraTypes()) ?
				JSON.parseArray(productEntity.getCameraTypes(), String.class) :
				Collections.emptyList());
		videoAnalysisDto.setDeviceId(String.valueOf(deviceId));
		videoAnalysisDto.setCallbackUrl(callbackUrl);

		if(videoAnalysisService.checkOnline(videoAnalysisDto).getCode() != HttpStatus.OK.value() ) {
			VideoAnalysisResponseVO register = videoAnalysisService.register(videoAnalysisDto);
			log.info("register:{}", register);
		}
	}
}
