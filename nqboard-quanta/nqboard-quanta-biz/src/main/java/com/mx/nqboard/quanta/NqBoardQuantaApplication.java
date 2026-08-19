package com.mx.nqboard.quanta;

import com.mx.nqboard.common.datasource.annotation.EnableDynamicDataSource;
import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 服务启动类
 *
 * @author SpicyRabbitLeg
 */
@EnableNqBoardDoc(value = "quanta")
@EnableNqBoardFeignClients
@EnableNqBoardResourceServer
@EnableDiscoveryClient
@SpringBootApplication
@EnableDynamicDataSource
public class NqBoardQuantaApplication {
	public static void main(String[] args) {
		SpringApplication.run(NqBoardQuantaApplication.class,args);
	}
}
