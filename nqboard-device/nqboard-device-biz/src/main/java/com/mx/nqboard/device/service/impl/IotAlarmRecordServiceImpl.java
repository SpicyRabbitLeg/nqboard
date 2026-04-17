package com.mx.nqboard.device.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.admin.api.dto.UploadFileDTO;
import com.mx.nqboard.admin.api.feign.RemoteFileService;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.core.util.RedisUtils;
import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.device.api.constant.IotAlarmRecordConstant;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.VideoAlarmRecordDTO;
import com.mx.nqboard.device.api.entity.IotAlarmRecordEntity;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.mapper.IotAlarmRecordMapper;
import com.mx.nqboard.device.service.IotAlarmRecordService;
import com.mx.nqboard.device.service.IotDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 告警记录管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotAlarmRecordServiceImpl extends ServiceImpl<IotAlarmRecordMapper, IotAlarmRecordEntity> implements IotAlarmRecordService {

    private final IotAlarmRecordMapper alarmRecordMapper;

    private final RemoteFileService remoteFileService;
    
    private final IotDeviceService deviceService;

    // Redis key前缀，用于5分钟内同一种recognize_type的防重
    private static final String ALARM_RECOGNIZE_KEY_PREFIX = "alarm:recognize:";
    
    // 5分钟防重时间（秒）
    private static final int ALARM_RECOGNIZE_EXPIRE_SECONDS = 300;


	@Override
	public Boolean callback(VideoAlarmRecordDTO alarmRecordDto) {
		if(StrUtil.isBlank(alarmRecordDto.getCallbackType())) {
			log.error("回调类型不能为空");
			return Boolean.FALSE;
		}

		if(IotAlarmRecordConstant.ALARM_VIDEO_TYPE_ONLINE.equals(alarmRecordDto.getCallbackType())) {
			// 修改设备在线状态
			return handleStatusCallback(alarmRecordDto);
		} else if (IotAlarmRecordConstant.ALARM_VIDEO_TYPE_RECOGNIZE.equals(alarmRecordDto.getCallbackType())) {
			// 识别告警处理
			return handleRecognizeCallback(alarmRecordDto);
		} else {
			log.error("未适配的回调类型: {}", alarmRecordDto.getCallbackType());
		}
		return Boolean.TRUE;
	}
	
	/**
	 * 处理STATUS类型回调
	 */
	private Boolean handleStatusCallback(VideoAlarmRecordDTO alarmRecordDto) {
		try {
			String deviceId = alarmRecordDto.getDeviceId();
			if (StrUtil.isBlank(deviceId)) {
				log.error("STATUS回调设备ID不能为空");
				return Boolean.FALSE;
			}
			
			// 更新设备在线状态到Redis
			RedisUtils.set(String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, deviceId), StringConstants.Switch.ENABLE,1,TimeUnit.MINUTES);
			log.info("设备在线状态更新成功，设备ID: {}, 状态: {}", deviceId, alarmRecordDto.getDeviceStatus());
			return Boolean.TRUE;
		} catch (Exception e) {
			log.error("处理STATUS回调失败", e);
			return Boolean.FALSE;
		}
	}
	
	/**
	 * 处理RECOGNIZE类型回调
	 */
	private Boolean handleRecognizeCallback(VideoAlarmRecordDTO alarmRecordDto) {
		try {
			String deviceId = alarmRecordDto.getDeviceId();
			String recognizeType = alarmRecordDto.getRecognizeType();
			String frameBase64 = alarmRecordDto.getFrameBase64();
			
			// 参数校验
			if (StrUtil.isBlank(deviceId)) {
				log.error("RECOGNIZE回调设备ID不能为空");
				return Boolean.FALSE;
			}
			if (StrUtil.isBlank(recognizeType)) {
				log.error("RECOGNIZE回调识别类型不能为空");
				return Boolean.FALSE;
			}
			if (StrUtil.isBlank(frameBase64)) {
				log.error("RECOGNIZE回调图片Base64不能为空");
				return Boolean.FALSE;
			}
			
			// 1. 上传图片获取URI
			String imageUri = uploadImage(frameBase64, recognizeType);
			if (StrUtil.isBlank(imageUri)) {
				log.error("图片上传失败");
				return Boolean.FALSE;
			}
			
			// 2. 根据设备ID查询用户ID
			Long userId = getUserIdByDeviceId(deviceId);
			if (userId == null) {
				log.error("未找到设备对应的用户ID，设备ID: {}", deviceId);
				// 如果找不到用户ID，使用默认值1
				userId = 1L;
			}
			
			// 3. 检查5分钟内是否已有相同识别类型的记录
			String redisKey = ALARM_RECOGNIZE_KEY_PREFIX + deviceId + ":" + recognizeType;
			String existingRecordId = RedisUtils.get(redisKey);
			
			if (StrUtil.isNotBlank(existingRecordId)) {
				// 已有记录，更新现有记录（追加图片）
				return updateExistingAlarmRecord(existingRecordId, imageUri, alarmRecordDto);
			} else {
				// 没有记录，创建新记录
				return createNewAlarmRecord(alarmRecordDto, imageUri, userId, redisKey);
			}
		} catch (Exception e) {
			log.error("处理RECOGNIZE回调失败", e);
			return Boolean.FALSE;
		}
	}
	
	/**
	 * 上传图片到文件服务
	 */
	private String uploadImage(String frameBase64, String recognizeType) {
		try {
			// 生成文件名：识别类型_时间戳
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
			String fileName = recognizeType + "_" + timestamp + ".jpg";
			
			UploadFileDTO uploadFileDto = new UploadFileDTO();
			uploadFileDto.setBase64Data(frameBase64);
			uploadFileDto.setFileName(fileName);
			uploadFileDto.setFileType("image/jpeg");
			
			R<Map<String, String>> result = remoteFileService.uploadBase64(uploadFileDto);
			
			return RetOps.of(result)
					.assertSuccess(r -> new RuntimeException("图片上传失败: " + r.getMsg()))
					.getData()
					.map(data -> data.get("url"))
					.orElse(null);
		} catch (Exception e) {
			log.error("上传图片失败", e);
			return null;
		}
	}
	
		/**
	 * 根据设备ID查询用户ID
	 */
	private Long getUserIdByDeviceId(String deviceId) {
		try {
			// 解析设备ID为Long类型
			long deviceIdLong;
			try {
				deviceIdLong = Long.parseLong(deviceId);
			} catch (NumberFormatException e) {
				log.error("设备ID格式错误: {}", deviceId);
				return null;
			}
			
			// 查询设备信息
			IotDeviceEntity device = deviceService.getById(deviceIdLong);
			if (device == null) {
				log.error("未找到设备，设备ID: {}", deviceId);
				return null;
			}
			
			return device.getUserId();
		} catch (Exception e) {
			log.error("查询设备用户ID失败，设备ID: {}", deviceId, e);
			return null;
		}
	}
	
	/**
	 * 创建新的告警记录
	 */
	private Boolean createNewAlarmRecord(VideoAlarmRecordDTO alarmRecordDto, String imageUri, Long userId, String redisKey) {
		try {
			// 生成告警消息
			String message = generateAlarmMessage(alarmRecordDto);
			
			IotAlarmRecordEntity alarmRecord = new IotAlarmRecordEntity();
			alarmRecord.setDeviceId(Long.parseLong(alarmRecordDto.getDeviceId()));
			alarmRecord.setUserId(userId);
			alarmRecord.setMessage(message);
			alarmRecord.setImgStr(imageUri);
			alarmRecord.setHandleStatus("0"); // 0未处理
			
			boolean saveResult = save(alarmRecord);
			if (saveResult && alarmRecord.getId() != null) {
				// 保存成功，设置Redis防重key，5分钟过期
				RedisUtils.set(redisKey, alarmRecord.getId().toString(), ALARM_RECOGNIZE_EXPIRE_SECONDS, TimeUnit.SECONDS);
				log.info("创建告警记录成功，ID: {}, 设备ID: {}, 识别类型: {}", 
						alarmRecord.getId(), alarmRecordDto.getDeviceId(), alarmRecordDto.getRecognizeType());
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		} catch (Exception e) {
			log.error("创建告警记录失败", e);
			return Boolean.FALSE;
		}
	}
	
	/**
	 * 更新现有的告警记录（追加图片）
	 */
	private Boolean updateExistingAlarmRecord(String recordId, String newImageUri, VideoAlarmRecordDTO alarmRecordDto) {
		try {
			Long id = Long.parseLong(recordId);
			IotAlarmRecordEntity existingRecord = getById(id);
			
			if (existingRecord == null) {
				log.error("未找到告警记录，ID: {}", recordId);
				return Boolean.FALSE;
			}
			
			// 追加新图片URI到imgStr字段（用逗号分隔）
			String existingImgStr = existingRecord.getImgStr();
			String updatedImgStr = StrUtil.isBlank(existingImgStr) ? 
					newImageUri : existingImgStr + "," + newImageUri;
			
			existingRecord.setImgStr(updatedImgStr);
			boolean updateResult = updateById(existingRecord);
			
			if (updateResult) {
				log.info("更新告警记录成功，追加图片，记录ID: {}, 设备ID: {}", 
						recordId, alarmRecordDto.getDeviceId());
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		} catch (Exception e) {
			log.error("更新告警记录失败", e);
			return Boolean.FALSE;
		}
	}
	
	/**
	 * 生成告警消息
	 */
	private String generateAlarmMessage(VideoAlarmRecordDTO alarmRecordDto) {
		StringBuilder message = new StringBuilder();
		message.append("识别类型: ").append(alarmRecordDto.getRecognizeType());
		
		if (alarmRecordDto.getTargetLocation() != null) {
			message.append(", 目标位置: ").append(JSON.toJSONString(alarmRecordDto.getTargetLocation()));
		}
		
		if (alarmRecordDto.getConfidence() != null) {
			message.append(", 置信度: ").append(String.format("%.2f", alarmRecordDto.getConfidence()));
		}
		
		if (alarmRecordDto.getFrameTime() != null) {
			message.append(", 帧时间: ").append(alarmRecordDto.getFrameTime());
		}
		
		return message.toString();
	}
}
