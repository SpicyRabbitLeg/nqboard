package com.mx.nqboard.workflow;

import com.mx.nqboard.common.datasource.annotation.EnableDynamicDataSource;
import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author 泥鳅压滑板
 */
@Slf4j
@EnableNqBoardDoc(value = "workflow")
@EnableNqBoardFeignClients
@EnableNqBoardResourceServer
@EnableDiscoveryClient
@SpringBootApplication
@EnableDynamicDataSource
public class NqBoardWorkFlowApplication {
	public static void main(String[] args) {
		SpringApplication.run(NqBoardWorkFlowApplication.class, args);
	}
}
