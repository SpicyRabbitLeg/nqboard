package com.mx.nqboard.daemon.quartz;

import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PigQuartz应用启动类
 * <p>
 * 集成定时任务、Feign客户端、资源服务及服务发现功能
 *
 * @author lengleng
 * @author frwcloud
 * @date 2025/05/31
 */
@EnableNqBoardDoc("job")
@EnableNqBoardFeignClients
@EnableNqBoardResourceServer
@EnableDiscoveryClient
@SpringBootApplication
public class NqBoardQuartzApplication {

	public static void main(String[] args) {
		SpringApplication.run(NqBoardQuartzApplication.class, args);
	}

}
