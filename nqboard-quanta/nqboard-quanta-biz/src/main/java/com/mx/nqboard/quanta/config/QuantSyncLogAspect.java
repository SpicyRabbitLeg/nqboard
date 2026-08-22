package com.mx.nqboard.quanta.config;

import cn.hutool.core.util.StrUtil;
import com.mx.nqboard.quanta.api.dto.SyncResult;
import com.mx.nqboard.quanta.api.entity.QuantSyncLogEntity;
import com.mx.nqboard.quanta.mapper.QuantSyncLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * 数据同步日志切面
 * </p>
 * <p>
 * 拦截 {@link QuantSyncLog} 标注的同步方法，逐步骤落 {@code quant_sync_log}
 * （RUNNING -> SUCCESS/FAILED，含成功/失败条数、同步区间、耗时），供前端按时间追溯。
 * 日志落库失败不影响同步本身（降级为仅打 error 日志）。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QuantSyncLogAspect {

	private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	/**
	 * status：运行中
	 */
	private static final String STATUS_RUNNING = "RUNNING";

	/**
	 * status：成功
	 */
	private static final String STATUS_SUCCESS = "SUCCESS";

	/**
	 * status：失败
	 */
	private static final String STATUS_FAILED = "FAILED";

	private final QuantSyncLogMapper quantSyncLogMapper;

	@Around("@annotation(quantSyncLog)")
	public Object around(ProceedingJoinPoint point, QuantSyncLog quantSyncLog) throws Throwable {
		LocalDateTime begin = LocalDateTime.now();
		QuantSyncLogEntity logEntry = insertRunning(quantSyncLog, begin);
		try {
			Object result = point.proceed();
			finish(logEntry, begin, result, null);
			return result;
		}
		catch (Throwable e) {
			finish(logEntry, begin, null, e);
			throw e;
		}
	}

	/**
	 * 落 RUNNING 起始行（失败降级为返回 null，后续回写跳过）
	 */
	private QuantSyncLogEntity insertRunning(QuantSyncLog quantSyncLog, LocalDateTime begin) {
		QuantSyncLogEntity logEntry = new QuantSyncLogEntity();
		logEntry.setSyncType(quantSyncLog.type());
		logEntry.setSyncName(quantSyncLog.name());
		logEntry.setRunDate(LocalDate.now().format(BASIC_DATE));
		logEntry.setStatus(STATUS_RUNNING);
		logEntry.setBeginTime(begin);
		try {
			quantSyncLogMapper.insert(logEntry);
			return logEntry;
		}
		catch (Exception e) {
			log.error("同步日志起始行落库失败（不影响同步）, syncType={}: {}", quantSyncLog.type(), e.getMessage());
			return null;
		}
	}

	/**
	 * 回写终态：正常返回取 SyncResult/数值计数，异常回写 FAILED
	 */
	private void finish(QuantSyncLogEntity logEntry, LocalDateTime begin, Object result, Throwable error) {
		if (logEntry == null) {
			return;
		}
		LocalDateTime end = LocalDateTime.now();
		logEntry.setEndTime(end);
		logEntry.setElapsedMs(Duration.between(begin, end).toMillis());
		if (error != null) {
			logEntry.setStatus(STATUS_FAILED);
			logEntry.setException(StrUtil.maxLength(error.toString(), 2000));
			logEntry.setMessage("同步失败: " + error.getMessage());
		}
		else {
			logEntry.setStatus(STATUS_SUCCESS);
			applyResult(logEntry, result);
		}
		try {
			quantSyncLogMapper.updateById(logEntry);
		}
		catch (Exception e) {
			log.error("同步日志终态回写失败（不影响同步）, syncType={}: {}", logEntry.getSyncType(), e.getMessage());
		}
	}

	/**
	 * 从返回值提取计数：SyncResult 取全量口径；Integer/int 兼容旧签名（仅成功条数）
	 */
	private void applyResult(QuantSyncLogEntity logEntry, Object result) {
		if (result instanceof SyncResult syncResult) {
			logEntry.setSuccessCount(syncResult.getSuccessCount());
			logEntry.setFailCount(syncResult.getFailCount());
			logEntry.setTotalCount(syncResult.getTotalCount() != null ? syncResult.getTotalCount()
					: syncResult.getSuccessCount() + syncResult.getFailCount());
			logEntry.setSyncRange(syncResult.getSyncRange());
			logEntry.setMessage(StrUtil.blankToDefault(syncResult.getMessage(),
					"同步成功 " + syncResult.getSuccessCount() + " 条, 失败 " + syncResult.getFailCount() + " 条"));
		}
		else if (result instanceof Integer count) {
			logEntry.setSuccessCount(count);
			logEntry.setTotalCount(count);
			logEntry.setMessage("同步成功 " + count + " 条");
		}
	}

}
