package com.mx.nqboard.device.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mx.nqboard.common.core.constant.CacheConstants;
import com.mx.nqboard.common.core.constant.StringConstants;
import com.mx.nqboard.common.core.util.MsgUtils;
import com.mx.nqboard.common.core.util.RedisUtils;
import com.mx.nqboard.common.security.util.SecurityUtils;
import com.mx.nqboard.device.api.constant.IotDeviceConstant;
import com.mx.nqboard.device.api.dto.*;
import com.mx.nqboard.device.api.entity.IotDeviceEntity;
import com.mx.nqboard.device.api.entity.IotPointEntity;
import com.mx.nqboard.device.api.entity.IotProductEntity;
import com.mx.nqboard.device.api.vo.IotDeviceVO;
import com.mx.nqboard.device.api.vo.IotPointVO;
import com.mx.nqboard.device.mapper.IotDeviceMapper;
import com.mx.nqboard.device.mapper.IotPointMapper;
import com.mx.nqboard.device.mapper.IotProductMapper;
import com.mx.nqboard.device.service.*;
import com.mx.nqboard.device.utils.ColumnNameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 设备管理 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotDeviceServiceImpl extends ServiceImpl<IotDeviceMapper, IotDeviceEntity> implements IotDeviceService {
    private final IotDeviceMapper deviceMapper;

    private final IotPointMapper pointMapper;

    private final IotProductMapper productMapper;

    private final RocketMQTemplate rocketMQTemplate;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final IotDynamicTableService dynamicTableService;

    private final IotPointService pointService;

    private final IotProductService productService;

    private final DriverCustomHandler driverCustomHandler;

    private final VideoAnalysisService videoAnalysisService;

    @Override
    public IPage<IotDeviceVO> page(Page page, IotDeviceDTO entity) {
        Long userId = SecurityUtils.getUser().getId();
        entity.setUserId(userId);

        if (StrUtil.isNotBlank(entity.getStatus())) {
            if (IotDeviceConstant.DEVICE_ONLINE_STATUS.equals(entity.getStatus())) {
                List<Long> deviceIds = RedisUtils.getKeys(IotDeviceConstant.DEVICE_ONLINE_ALL_KEY)
                        .stream().map(key -> Long.parseLong(key.replace(IotDeviceConstant.DEVICE_ONLINE_ALL_REPLACE_KEY, "")))
                        .collect(Collectors.toList());
                entity.setOnlineDeviceIds(deviceIds);
                entity.setStatus(null);
            } else if (IotDeviceConstant.DEVICE_OFFLINE_STATUS.equals(entity.getStatus())) {
                List<Long> deviceIds = RedisUtils.getKeys(IotDeviceConstant.DEVICE_ONLINE_ALL_KEY)
                        .stream().map(key -> Long.parseLong(key.replace(IotDeviceConstant.DEVICE_ONLINE_ALL_REPLACE_KEY, "")))
                        .collect(Collectors.toList());
                entity.setOfflineDeviceIds(deviceIds);
                entity.setStatus(null);
            }
        }
        LambdaQueryWrapper<IotDeviceEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.like(StrUtil.isNotBlank(entity.getName()), IotDeviceEntity::getName, entity.getName());
        queryWrapper.eq(StrUtil.isNotBlank(entity.getStatus()), IotDeviceEntity::getStatus, entity.getStatus());
        queryWrapper.eq(ObjectUtil.isNotNull(entity.getUserId()), IotDeviceEntity::getUserId, entity.getUserId());
        queryWrapper.eq(ObjectUtil.isNotNull(entity.getProductId()), IotDeviceEntity::getProductId, entity.getProductId());
        queryWrapper.in(ObjectUtil.isNotEmpty(entity.getOnlineDeviceIds()), IotDeviceEntity::getId, entity.getOnlineDeviceIds());
        queryWrapper.notIn(ObjectUtil.isNotEmpty(entity.getOfflineDeviceIds()), IotDeviceEntity::getId, entity.getOfflineDeviceIds());

        IPage<IotDeviceEntity> devicePage = deviceMapper.selectPage(page, queryWrapper);
        List<IotDeviceEntity> deviceRecords = devicePage.getRecords();

        Map<Long, IotProductEntity> productMap = Collections.emptyMap();
        if (ObjectUtil.isNotEmpty(deviceRecords)) {
            List<Long> productIds = deviceRecords.stream()
                    .map(IotDeviceEntity::getProductId)
                    .filter(ObjectUtil::isNotNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(productIds)) {
                productMap = productMapper.selectList(Wrappers.<IotProductEntity>lambdaQuery()
                                .in(IotProductEntity::getId, productIds))
                        .stream()
                        .collect(Collectors.toMap(IotProductEntity::getId, product -> product, (a, b) -> a));
            }
        }
        final Map<Long, IotProductEntity> finalProductMap = productMap;

        List<IotDeviceVO> voRecords = deviceRecords.stream().map(device -> {
            IotDeviceVO vo = BeanUtil.copyProperties(device, IotDeviceVO.class);
            vo.setProduct(finalProductMap.get(device.getProductId()));
            return vo;
        }).collect(Collectors.toList());

        Page<IotDeviceVO> resultPage = new Page<>(devicePage.getCurrent(), devicePage.getSize(), devicePage.getTotal());
        resultPage.setRecords(voRecords);

        // 查询在线状态
        for (IotDeviceVO record : resultPage.getRecords()) {
            String online = RedisUtils.get(String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, record.getId()));
            record.setOnline(StringConstants.Switch.ENABLE.equals(online));
        }
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        LambdaQueryWrapper<IotDeviceEntity> delete = Wrappers.lambdaQuery();
        delete.in(IotDeviceEntity::getId, ids)
                .eq(IotDeviceEntity::getUserId, SecurityUtils.getUser().getId());
        deviceMapper.delete(delete);


        LambdaQueryWrapper<IotPointEntity> deletePoint = Wrappers.lambdaQuery();
        deletePoint.in(IotPointEntity::getId, ids);
        pointMapper.delete(deletePoint);
        return Boolean.TRUE;
    }

    @Override
	@CacheEvict(value = CacheConstants.DEVICE_ALL_DETAILS, allEntries = true)
    public Boolean saveDevice(IotDeviceEntity iotDevice) {
        ColumnNameValidator.validateColumnName(iotDevice.getIdentifier());
        // 校验 identifier是否唯一
        Boolean tableExit = dynamicTableService.exitTable(iotDevice.getIdentifier());
        Assert.isTrue(!tableExit, MsgUtils.getMessage(IotDeviceConstant.ERROR_IDENTIFIER));

        // 采用事务消息实现
        TransactionDTO<IotDeviceDTO> transactionDto = new TransactionDTO<>();
        IotDeviceDTO iotDeviceDto = BeanUtil.copyProperties(iotDevice, IotDeviceDTO.class);
        iotDeviceDto.setId(IdWorker.getId());
        transactionDto.setBusinessType(IotDeviceConstant.DEVICE_SAVE_TOPIC);
        transactionDto.setBizObject(iotDeviceDto);

        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(transactionDto)).build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(IotDeviceConstant.DEVICE_SAVE_TOPIC, message, null);
        Assert.isTrue(SendStatus.SEND_OK.equals(result.getSendStatus()), IotDeviceConstant.ERROR_DEVICE_SAVE);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transactionalSaveDevice(IotDeviceDTO deviceDto) {
        return SqlHelper.retBool(deviceMapper.insert(deviceDto));
    }

    @Override
	@CacheEvict(value = CacheConstants.DEVICE_ALL_DETAILS, allEntries = true)
    public Boolean deleteDeviceByIds(List<Long> ids) {
        for (IotDeviceEntity deviceEntity : deviceMapper.selectBatchIds(ids)) {
            Assert.isTrue(dynamicTableService.exitTable(deviceEntity.getIdentifier()), MsgUtils.getMessage(IotDeviceConstant.ERROR_DEVICE_EXIT));

            // 采用事务消息实现
            TransactionDTO<IotDeviceDTO> transactionDto = new TransactionDTO<>();
            IotDeviceDTO iotDeviceDto = BeanUtil.copyProperties(deviceEntity, IotDeviceDTO.class);
            transactionDto.setBusinessType(IotDeviceConstant.DEVICE_DEL_TOPIC);
            transactionDto.setBizObject(iotDeviceDto);
            Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(transactionDto)).build();

            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(IotDeviceConstant.DEVICE_DEL_TOPIC, message, null);
            Assert.isTrue(SendStatus.SEND_OK.equals(result.getSendStatus()), MsgUtils.getMessage(IotDeviceConstant.ERROR_DEL));
        }
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transactionalDelDevice(IotDeviceDTO deviceDto) {
		LambdaQueryWrapper<IotPointEntity> pointWrapper = Wrappers.lambdaQuery();
		pointWrapper.eq(IotPointEntity::getDeviceId,deviceDto.getId());
		pointMapper.delete(pointWrapper);

		return SqlHelper.retBool(deviceMapper.deleteById(deviceDto.getId()));
    }

    @Override
    public void setOnlineStatus(String value, Long deviceId) {
        CompletableFuture.runAsync(() -> {
            final String deviceOnlineKey = String.format(IotDeviceConstant.DEVICE_ONLINE_KEY, deviceId);

            String online = RedisUtils.get(deviceOnlineKey);
            if (ObjectUtil.isNull(online) && StrUtil.isNotBlank(value)) {
                RedisUtils.set(deviceOnlineKey, StringConstants.Switch.ENABLE, 1, TimeUnit.MINUTES);
            }
        }, threadPoolTaskExecutor);
    }

    @Override
    public List<Tree<String>> getDeviceByGroup(IotDeviceDTO iotDeviceDto) {
        List<IotProductEntity> productList = productMapper.selectList(Wrappers.emptyWrapper());

        LambdaQueryWrapper<IotDeviceEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.like(StrUtil.isNotBlank(iotDeviceDto.getName()),IotDeviceEntity::getName,iotDeviceDto.getName());
        queryWrapper.eq(IotDeviceEntity::getUserId,SecurityUtils.getUser().getId());
        List<IotDeviceEntity> deviceList = deviceMapper.selectList(queryWrapper);


        List<TreeNode<String>> nodeList = new ArrayList<>();
        for (IotProductEntity productEntity : productList) {
            nodeList.add(new TreeNode<>(
                    productEntity.getId().toString(),
                    "0",
                    productEntity.getName(),
                    Optional.ofNullable(productEntity.getOrderNum()).orElse(0)
            ));
        }

        for (IotDeviceEntity deviceEntity : deviceList) {
            nodeList.add(new TreeNode<>(
                    deviceEntity.getId().toString(),
                    deviceEntity.getProductId().toString(),
                    deviceEntity.getName(),
                    Optional.ofNullable(deviceEntity.getOrderNum()).orElse(0)
            ));
        }
        return TreeUtil.build(nodeList, "0");
    }

    @Override
    public Boolean writeDevice(WriteDeviceDTO writeDeviceDTO) {
        log.info("设备写入操作开始，设备ID: {}", writeDeviceDTO.getDeviceId());
        
        try {
            // 1. 验证设备和产品信息
            DeviceProductInfoDTO deviceProductInfo = validateAndGetDeviceProductInfo(writeDeviceDTO.getDeviceId());
            
            // 2. 获取设备数据点并解析驱动信息
            IotPointEntity pointQuery = new IotPointEntity();
            pointQuery.setDeviceId(Long.parseLong(writeDeviceDTO.getDeviceId()));
            List<IotPointVO> devicePoints = pointService.getPointDetailByDeviceId(pointQuery);
            Map<String, Object> driverInfo = JSON.parseObject(deviceProductInfo.getDevice().getDriverValue());
            
            // 3. 执行数据点写入
            executePointsWrite(writeDeviceDTO, deviceProductInfo, devicePoints, driverInfo);
            
            log.info("设备写入完成，设备: {}, 数据点数量: {}", deviceProductInfo.getDevice().getName(), writeDeviceDTO.getData().size());
            return true;
        } catch (Exception e) {
            log.error("设备写入失败，设备ID: {}, 错误: {}", writeDeviceDTO.getDeviceId(), e.getMessage());
            throw new RuntimeException("设备写入失败: " + e.getMessage());
        }
    }

	@Override
	public Boolean deleteVideoByDevice(Long[] ids) {
		for (Long id : ids) {
			VideoAnalysisDTO dto = new VideoAnalysisDTO();
			dto.setDeviceId(String.valueOf(id));
			videoAnalysisService.delete(dto);
		}
		return Boolean.TRUE;
	}

	/**
     * 验证设备和产品信息
     */
    private DeviceProductInfoDTO validateAndGetDeviceProductInfo(String deviceId) {
        Long userId = SecurityUtils.getUser().getId();
        
        IotDeviceEntity device = deviceMapper.selectOne(
            Wrappers.<IotDeviceEntity>lambdaQuery()
                .eq(IotDeviceEntity::getId, deviceId)
                .eq(IotDeviceEntity::getUserId, userId)
        );
        
        Assert.notNull(device, "设备不存在或无权限访问");
        Assert.isTrue("1".equals(device.getStatus()), "设备未启用，无法执行写入操作");
        
        IotProductEntity product = productService.getById(device.getProductId());
        Assert.notNull(product, "设备关联的产品不存在");
        
        return new DeviceProductInfoDTO(device, product);
    }

    /**
     * 执行数据点写入
     */
    private void executePointsWrite(WriteDeviceDTO writeDeviceDTO, DeviceProductInfoDTO deviceProductInfo,
                                   List<IotPointVO> devicePoints, Map<String, Object> driverInfo) {
        String driverType = deviceProductInfo.getProduct().getProtocol();
        
        for (String identifier : writeDeviceDTO.getData().keySet()) {
            IotPointVO targetPoint = findPointByIdentifier(devicePoints, identifier);
            validatePointWritable(targetPoint, identifier, writeDeviceDTO.getDataTypes());
            
            Map<String, Object> pointInfo = buildPointInfo(targetPoint);
            pointInfo.put("deviceId",writeDeviceDTO.getDeviceId());
            Object writeValue = writeDeviceDTO.getData().get(identifier);
            
            Boolean writeResult = driverCustomHandler.write(driverType, driverInfo, pointInfo, writeValue);
            
            if (!writeResult) {
                throw new RuntimeException("数据点 '" + identifier + "' 写入失败");
            }
        }
    }

    /**
     * 根据标识符查找数据点
     */
    private IotPointVO findPointByIdentifier(List<IotPointVO> devicePoints, String identifier) {
        return devicePoints.stream()
            .filter(point -> identifier.equals(point.getModel().getIdentifier()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("数据点 '" + identifier + "' 在该设备中不存在"));
    }

    /**
     * 验证数据点可写性和数据类型
     */
    private void validatePointWritable(IotPointVO targetPoint, String identifier, Map<String, String> dataTypes) {
        Assert.isTrue(!"1".equals(targetPoint.getModel().getType()), "数据点 '" + identifier + "' 不可写");
        
        String expectedDataType = targetPoint.getModel().getDataType();
        String providedDataType = dataTypes.get(identifier);
        
        if (StrUtil.isAllNotBlank(expectedDataType, providedDataType)) {
            Assert.isTrue(expectedDataType.equals(providedDataType), 
                "数据点 '" + identifier + "' 数据类型不匹配，期望: " + expectedDataType + ", 实际: " + providedDataType);
        }
    }

    /**
     * 构建点位信息
     */
    private Map<String, Object> buildPointInfo(IotPointVO targetPoint) {
        Map<String, Object> pointInfo = targetPoint.getPointInfo();
        if (pointInfo == null) {
            pointInfo = new HashMap<>(16);
        }
        pointInfo.put("dataType", targetPoint.getModel().getDataType());
        return pointInfo;
    }
}
