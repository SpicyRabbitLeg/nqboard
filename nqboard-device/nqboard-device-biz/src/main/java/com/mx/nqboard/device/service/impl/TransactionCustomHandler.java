package com.mx.nqboard.device.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.mx.nqboard.device.service.TransactionCustomService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 策略模式
 * 分布式事务消息
 *
 * @author SpicyRabbitLeg
 */
@Log4j2
@Service
public class TransactionCustomHandler {
    private final Map<String, TransactionCustomService<?>> handlers = new HashMap<>(16);

    /**
     * 注册链接协议接口
     *
     * @param driverType               driverType
     * @param transactionCustomService transactionCustomService
     */
    public void register(String driverType, TransactionCustomService<?> transactionCustomService) {
        handlers.put(driverType, transactionCustomService);
    }

    /**
     * 执行本地事务
     *
     * @param businessType 业务类型
     * @param bizObject    消息内容（JSONObject类型）
     * @return 本地事务是否成功
     */
    @SuppressWarnings("unchecked")
    public boolean executeLocalTransaction(String businessType, Object bizObject) {
        TransactionCustomService<?> handler = Optional.ofNullable(handlers.get(businessType))
                .orElseThrow(() -> new IllegalArgumentException("未找到对应的事务处理器: " + businessType));

        Object targetObject = convertBizObject(bizObject, handler);
        return ((TransactionCustomService<Object>) handler).executeLocalTransaction(targetObject);
    }

    /**
     * 事务回查（增加类型转换）
     *
     * @param businessType 业务类型
     * @param bizObject    消息内容（JSONObject类型）
     * @return 本地事务是否成功
     */
    @SuppressWarnings("unchecked")
    public boolean checkLocalTransaction(String businessType, Object bizObject) {
        TransactionCustomService<?> handler = Optional.ofNullable(handlers.get(businessType))
                .orElseThrow(() -> new IllegalArgumentException("未找到对应的事务处理器: " + businessType));
        Object targetObject = convertBizObject(bizObject, handler);
        return ((TransactionCustomService<Object>) handler).checkLocalTransaction(targetObject);
    }

    /**
     * 核心转换方法：将JSONObject转换为处理器需要的目标类型
     *
     * @param bizObject 原始的JSONObject对象
     * @param handler   具体的事务处理器
     * @return 转换后的目标类型对象
     */
    private Object convertBizObject(Object bizObject, TransactionCustomService<?> handler) {
        try {
            // 1. 获取处理器的泛型类型（如IotProductDTO）
            Class<?> targetType = getGenericType(handler.getClass());

            // 2. 分情况转换
            if (bizObject instanceof JSONObject) {
                return ((JSONObject) bizObject).toJavaObject(targetType);
            } else if (targetType.isInstance(bizObject)) {
                return bizObject;
            } else {
                String jsonStr = bizObject.toString();
                return JSONObject.parseObject(jsonStr, targetType);
            }
        } catch (Exception e) {
            log.error("转换业务对象失败, bizObject: {}, handler: {}", bizObject, handler.getClass().getName(), e);
            throw new RuntimeException("业务对象类型转换失败", e);
        }
    }

    /**
     * 获取TransactionCustomService的泛型参数类型
     * 例如：TransactionCustomProductSaveServiceImpl -> IotProductDTO
     *
     * @param clazz 处理器实现类
     * @return 泛型参数类型
     */
    private Class<?> getGenericType(Class<?> clazz) {
        // 遍历父接口，找到TransactionCustomService的泛型定义
        for (java.lang.reflect.Type interfaceType : clazz.getGenericInterfaces()) {
            if (interfaceType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType parameterizedType = (java.lang.reflect.ParameterizedType) interfaceType;
                if (parameterizedType.getRawType() == TransactionCustomService.class) {
                    // 获取泛型参数（如IotProductDTO）
                    java.lang.reflect.Type actualType = parameterizedType.getActualTypeArguments()[0];
                    if (actualType instanceof Class<?>) {
                        return (Class<?>) actualType;
                    }
                }
            }
        }
        throw new IllegalArgumentException("未找到TransactionCustomService的泛型类型: " + clazz.getName());
    }
}