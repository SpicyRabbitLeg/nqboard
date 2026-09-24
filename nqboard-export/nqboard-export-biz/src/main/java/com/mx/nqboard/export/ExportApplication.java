package com.mx.nqboard.export;

import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 智能专家抽取模块
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
@EnableNqBoardDoc(value = "export")
@EnableNqBoardFeignClients
@EnableNqBoardResourceServer
@EnableDiscoveryClient
@SpringBootApplication
public class ExportApplication {
	public static void main(String[] args) {
		SpringApplication.run(ExportApplication.class, args);
	}
}
