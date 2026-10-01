package com.mx.nqboard.sniper;

import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 短线股票分析模块
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/01
 */
@EnableNqBoardDoc(value = "sniper")
@EnableNqBoardFeignClients
@EnableNqBoardResourceServer
@EnableDiscoveryClient
@SpringBootApplication
public class SniperApplication {
	public static void main(String[] args) {
		SpringApplication.run(SniperApplication.class, args);
	}
}
