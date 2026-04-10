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

import com.mx.nqboard.daemon.quartz.constants.NqBoardQuartzEnum;
import com.mx.nqboard.daemon.quartz.entity.SysJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.SneakyThrows;

/**
 * 动态任务工厂：用于执行动态任务调度
 *
 * @author lengleng
 * @author 郑健楠
 * @date 2025/05/31
 */
@DisallowConcurrentExecution
public class NqBoardQuartzFactory implements Job {

	/**
	 * 定时任务调用工厂
	 */
	@Autowired
	private NqBoardQuartzInvokeFactory nqBoardQuartzInvokeFactory;

	/**
	 * 执行定时任务
	 * @param jobExecutionContext 任务执行上下文
	 * @throws Exception 执行过程中可能抛出的异常
	 */
	@Override
	@SneakyThrows
	public void execute(JobExecutionContext jobExecutionContext) {
		SysJob sysJob = (SysJob) jobExecutionContext.getMergedJobDataMap()
			.get(NqBoardQuartzEnum.SCHEDULE_JOB_KEY.getType());
		nqBoardQuartzInvokeFactory.init(sysJob, jobExecutionContext.getTrigger());
	}

}
