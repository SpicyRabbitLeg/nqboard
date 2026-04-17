package com.mx.nqboard.common.rocketmq.config;


import com.mx.nqboard.common.core.factory.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * rocketmq 配置类
 *
 * @author SpicyRabbitLeg
 */
@PropertySource(value = "classpath:rocketmq-config.yml", factory = YamlPropertySourceFactory.class)
@Configuration(proxyBeanMethods = false)
public class RocketMqAutoConfiguration {
}
