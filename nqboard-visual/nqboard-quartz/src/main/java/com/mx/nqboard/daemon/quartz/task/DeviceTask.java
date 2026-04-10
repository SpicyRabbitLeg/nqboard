package com.mx.nqboard.daemon.quartz.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.mx.nqboard.common.core.util.RedisUtils;
import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.daemon.quartz.constants.NqBoardQuartzEnum;
import com.mx.nqboard.device.api.constant.IotProductConstant;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.feign.RemoteDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * 设备定时采集任务
 *
 * @author 泥鳅压滑板
 */
@Slf4j
@Component("device")
@RequiredArgsConstructor
public class DeviceTask {
	private static final String DEVICE_NEXT_TIME_KEY_TPL = "device:nextTime:%s";

	private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

	private final RemoteDeviceService remoteDeviceService;

	private final RocketMQTemplate rocketMQTemplate;

	/**
	 * 设备采集调度入口
	 */
	@SneakyThrows
	public String getDevice() {
		// 1. 获取设备列表（空值安全）
		List<IotDeviceEntity> deviceList = RetOps.of(remoteDeviceService.getDeviceAll())
				.getData()
				.orElse(Collections.emptyList());

		if (deviceList.isEmpty()) {
			log.debug("[设备定时任务] 暂无需要调度的设备");
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}

		// 2. 遍历处理设备
		deviceList.stream()
				.filter(device -> StrUtil.isNotBlank(device.getCron()))
				.forEach(this::handleDeviceSchedule);

		return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
	}

	/**
	 * 处理单个设备的采集调度逻辑
	 */
	private void handleDeviceSchedule(IotDeviceEntity device) {
		String redisKey = buildDeviceNextTimeKey(device.getId());
		LocalDateTime now = LocalDateTime.now(SHANGHAI_ZONE);
		LocalDateTime nextExecuteTime = RedisUtils.get(redisKey);

		// 无下次执行时间 → 初始化
		if (ObjectUtil.isEmpty(nextExecuteTime)) {
			updateNextExecuteTime(device, redisKey, now);
			log.info("[设备定时任务] 初始化执行时间成功 | 设备ID:{} | 名称:{} | 下次时间:{}",
					device.getId(), device.getName(), nextExecuteTime);
			return;
		}

		// 已到执行时间 → 发送采集消息
		if (now.isAfter(nextExecuteTime) || now.isEqual(nextExecuteTime)) {
			try {
				rocketMQTemplate.convertAndSend(IotProductConstant.COLLECTION_TOPIC, JSON.toJSONString(device));
				log.info("[设备定时任务] 发送采集消息成功 | 设备ID:{} | 名称:{}", device.getId(), device.getName());
			} catch (Exception e) {
				log.error("[设备定时任务] 发送采集消息失败 | 设备ID:{} | 异常:{}", device.getId(), e.getMessage(), e);
			}

			// 更新下一次执行时间
			updateNextExecuteTime(device, redisKey, now);
		}
	}

	/**
	 * 构建 Redis Key
	 */
	private String buildDeviceNextTimeKey(Long deviceId) {
		return String.format(DEVICE_NEXT_TIME_KEY_TPL, deviceId);
	}

	/**
	 * 更新设备下次执行时间到 Redis
	 */
	private void updateNextExecuteTime(IotDeviceEntity device, String redisKey, LocalDateTime now) {
		try {
			CronExpression cron = CronExpression.parse(device.getCron());
			LocalDateTime nextTime = cron.next(now);
			RedisUtils.set(redisKey, nextTime);
			log.info("[设备定时任务] 更新下次执行时间 | 设备ID:{} | 名称:{} | 下次时间:{}",
					device.getId(), device.getName(), nextTime);
		} catch (Exception e) {
			log.error("[设备定时任务] Cron表达式解析失败 | 设备ID:{} | Cron:{} | 异常:{}",
					device.getId(), device.getCron(), e.getMessage(), e);
		}
	}
}