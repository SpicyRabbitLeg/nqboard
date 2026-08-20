package com.mx.nqboard.daemon.quartz.task;

import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.daemon.quartz.constants.NqBoardQuartzEnum;
import com.mx.nqboard.quanta.api.feign.RemoteStockBasicService;
import com.mx.nqboard.quanta.api.feign.RemoteStockDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 股票基础信息同步定时任务
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Component("stockBasic")
@RequiredArgsConstructor
public class StockBasicTask {

	private final RemoteStockBasicService remoteStockBasicService;

	private final RemoteStockDailyService remoteStockDailyService;

	/**
	 * 股票基础信息同步调度入口（无参，同步默认市场：主板）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String sync() {
		log.info("[股票基础信息定时任务] 开始同步");
		try {
			Integer count = RetOps.of(remoteStockBasicService.syncFromTushare(null))
					.getData()
					.orElse(0);
			log.info("[股票基础信息定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}
		catch (Exception e) {
			log.error("[股票基础信息定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 股票日线信息同步调度入口（入参：）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncDaily() {
		log.info("[股票日线信息定时任务] 开始同步");
		try {
			Integer count = RetOps.of(remoteStockDailyService.syncFromTushare("全部",false))
					.getData()
					.orElse(0);
			log.info("[股票日线信息定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}
		catch (Exception e) {
			log.error("[股票日线信息定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}
}
