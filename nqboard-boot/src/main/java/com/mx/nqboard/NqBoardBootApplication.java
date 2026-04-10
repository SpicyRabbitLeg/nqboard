/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mx.nqboard;

import com.mx.nqboard.common.security.annotation.EnableNqBoardResourceServer;
import com.mx.nqboard.common.swagger.annotation.EnableNqBoardDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单体版本启动器，运行此模块即可启动整个系统
 *
 * @author lengleng
 * @date 2025/05/30
 */
@SpringBootApplication
@EnableNqBoardResourceServer
@EnableNqBoardDoc(value = "admin", isMicro = false)
public class NqBoardBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(NqBoardBootApplication.class, args);
	}

}
