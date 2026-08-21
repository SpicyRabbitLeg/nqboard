package com.mx.nqboard.daemon.quartz.task;

import com.mx.nqboard.common.core.util.RetOps;
import com.mx.nqboard.daemon.quartz.constants.NqBoardQuartzEnum;
import com.mx.nqboard.quanta.api.feign.*;
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

	private final RemoteStockConsWeightService remoteStockConsWeightService;

	private final RemoteStockMotHolderService remoteStockMotHolderService;

	private final RemoteStockMotHolderCountService remoteStockMotHolderCountService;

	private final RemoteStockTopListService remoteStockTopListService;

	private final RemoteStockIndexDailyService remoteStockIndexDailyService;

	private final RemoteStockMoneyFlowService remoteStockMoneyFlowService;

	private final RemoteStockIndustryDailyService remoteStockIndustryDailyService;

	private final RemoteStockRestrictedReleaseService remoteStockRestrictedReleaseService;

	private final RemoteTradeCalService remoteTradeCalService;

	/**
	 * 交易日闸门：今日非交易日返回 false（交易日历由 quanta 持久化，Feign 调用失败时放行，避免依赖故障阻断数据同步）
	 */
	private boolean isTradeDay() {
		return RetOps.of(remoteTradeCalService.isOpenToday()).getData().orElse(true);
	}

	/**
	 * 股票基础信息同步调度入口（无参，同步默认市场：仅主板，不含创业板/科创板）
	 * <p>
	 * 策略定位只做主板：universe 中混入的创业板成分股会在股票池阶段被静默跳过（符合预期）。
	 * </p>
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String sync() {
		log.info("[股票基础信息定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[股票基础信息定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockBasicService.syncFromTushare("主板"))
					.getData()
					.orElse(0);
			log.info("[股票基础信息定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[股票基础信息定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 股票日线信息同步调度入口（增量 + 自愈回看 7 天，市场=仅主板，与基础信息口径一致）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncDaily() {
		log.info("[股票日线信息定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[股票日线信息定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockDailyService.syncFromTushare("主板", false))
					.getData()
					.orElse(0);
			log.info("[股票日线信息定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[股票日线信息定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 指数成分股权重同步调度入口（入参：）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncConsWeight() {
		log.info("[指数成分股权重定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[指数成分股权重定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockConsWeightService.syncFromCsindex(null))
					.getData()
					.orElse(0);
			log.info("[指数成分股权重定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[指数成分股权重定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}


	/**
	 * 股东增减持同步调度入口（当前固定全量拉取，市场=主板）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncMotHolder() {
		log.info("[股东增减持重定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[股东增减持重定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockMotHolderService.syncFromTushare("主板", false))
					.getData()
					.orElse(0);
			log.info("[股东增减持重定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[股东增减持重定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}


	/**
	 * 同步股东户数同步调度入口（当前固定全量拉取，市场=主板）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncMotHolderCount() {
		log.info("[同步股东户数重定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[同步股东户数重定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockMotHolderCountService.syncFromTushare("主板", false))
					.getData()
					.orElse(0);
			log.info("[同步股东户数重定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[同步股东户数重定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 龙虎榜每日明细同步调度入口（当前固定全量拉取）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncTopList() {
		log.info("[龙虎榜定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[龙虎榜定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockTopListService.syncFromTushare(null, false))
					.getData()
					.orElse(0);
			log.info("[龙虎榜定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[龙虎榜定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 指数日线K线同步调度入口（一次性全量拉取，从 2026-01-01 起）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncIndexDaily() {
		log.info("[指数日线定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[指数日线定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockIndexDailyService.syncFromEastMoney(false))
					.getData()
					.orElse(0);
			log.info("[指数日线定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[指数日线定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 个股主力资金流同步调度入口（东财当日全市场快照，建议 17:30 后执行）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncMoneyFlow() {
		log.info("[主力资金流定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[主力资金流定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockMoneyFlowService.syncFromEastMoney(false))
					.getData()
					.orElse(0);
			log.info("[主力资金流定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[主力资金流定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 行业板块日线同步调度入口（东财板块K线，当前固定全量拉取，建议 17:40 后执行；
	 * 约 86 个板块 × 3 秒间隔，全量约 5 分钟）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncIndustryDaily() {
		log.info("[行业板块日线定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[行业板块日线定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockIndustryDailyService.syncFromEastMoney(false))
					.getData()
					.orElse(0);
			log.info("[行业板块日线定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[行业板块日线定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 限售解禁同步调度入口（tushare share_float，当前固定全量回补90天逐日拉取约45秒，
	 * 兼具漏跑自愈能力，建议 18:00 后执行）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncRestrictedRelease() {
		log.info("[限售解禁定时任务] 开始同步");
		try {
			if (!isTradeDay()) {
				log.info("[限售解禁定时任务] 今日非交易日，跳过执行");
				return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
			}
			Integer count = RetOps.of(remoteStockRestrictedReleaseService.syncFromTushare(false))
					.getData()
					.orElse(0);
			log.info("[限售解禁定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[限售解禁定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}

	/**
	 * 交易日历同步调度入口（每日开盘前执行，刷新节假日/调休日历；不受交易日闸门限制，
	 * 休市日也需要更新日历以便次日判断）
	 *
	 * @return 任务执行状态：0 成功 / 1 失败
	 */
	public String syncTradeCal() {
		log.info("[交易日历定时任务] 开始同步");
		try {
			Integer count = RetOps.of(remoteTradeCalService.syncFromTushare())
					.getData()
					.orElse(0);
			log.info("[交易日历定时任务] 同步完成, 成功处理 {} 条", count);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		} catch (Exception e) {
			log.error("[交易日历定时任务] 同步失败, 异常:{}", e.getMessage(), e);
			return NqBoardQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
	}
}
