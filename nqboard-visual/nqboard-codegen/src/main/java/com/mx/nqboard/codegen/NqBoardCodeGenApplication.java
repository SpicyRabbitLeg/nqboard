/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.mx.nqboard.codegen;

import com.mx.nqboard.common.datasource.annotation.EnableDynamicDataSource;
import com.mx.nqboard.common.feign.annotation.EnableNqBoardFeignClients;
import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 代码生成模块应用启动类
 *
 * @author lengleng
 * @date 2025/05/31
 */
@EnableDynamicDataSource
@EnableNqBoardFeignClients
@EnableNqBoardDoc("gen")
@EnableDiscoveryClient
@EnableNqBoardResourceServer
@SpringBootApplication
public class NqBoardCodeGenApplication {

	public static void main(String[] args) {
		SpringApplication.run(NqBoardCodeGenApplication.class, args);
	}

}
