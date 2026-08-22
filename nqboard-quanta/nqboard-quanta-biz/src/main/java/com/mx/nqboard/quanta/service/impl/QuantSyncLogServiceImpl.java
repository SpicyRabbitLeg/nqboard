package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.QuantSyncLogEntity;
import com.mx.nqboard.quanta.mapper.QuantSyncLogMapper;
import com.mx.nqboard.quanta.service.QuantSyncLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数据同步执行日志 服务实现类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Service
public class QuantSyncLogServiceImpl extends ServiceImpl<QuantSyncLogMapper, QuantSyncLogEntity>
		implements QuantSyncLogService {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	@Override
	public List<QuantSyncLogEntity> latestRuns(String runDate) {
		String date = StrUtil.blankToDefault(runDate, LocalDate.now().format(BASIC_DATE));
		List<QuantSyncLogEntity> logs = list(Wrappers.<QuantSyncLogEntity>lambdaQuery()
				.eq(QuantSyncLogEntity::getRunDate, date)
				.orderByDesc(QuantSyncLogEntity::getBeginTime));
		// 同一任务同日多次执行时仅保留最新一条（按开始时间倒序，首见即最新）
		Map<String, QuantSyncLogEntity> latest = new LinkedHashMap<>();
		for (QuantSyncLogEntity logEntry : logs) {
			latest.putIfAbsent(logEntry.getSyncType(), logEntry);
		}
		return List.copyOf(latest.values());
	}

}
