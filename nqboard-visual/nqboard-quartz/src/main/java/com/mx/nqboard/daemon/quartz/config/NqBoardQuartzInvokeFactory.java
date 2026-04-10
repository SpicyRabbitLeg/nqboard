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

package com.mx.nqboard.daemon.quartz.config;

import com.mx.nqboard.daemon.quartz.entity.SysJob;
import com.mx.nqboard.daemon.quartz.event.SysJobEvent;
import org.aspectj.lang.annotation.Aspect;
import org.quartz.Trigger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * 定时任务调用工厂类 用于初始化并发布定时任务事件
 *
 * @author lengleng
 * @date 2025/05/31
 */
@Aspect
@Service
@AllArgsConstructor
public class NqBoardQuartzInvokeFactory {

	private final ApplicationEventPublisher publisher;

	/**
	 * 初始化并发布定时任务事件
	 * @param sysJob 系统任务对象
	 * @param trigger 任务触发器
	 */
	@SneakyThrows
	void init(SysJob sysJob, Trigger trigger) {
		publisher.publishEvent(new SysJobEvent(sysJob, trigger));
	}

}
