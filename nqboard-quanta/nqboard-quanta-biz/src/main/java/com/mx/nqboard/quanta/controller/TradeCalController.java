package com.mx.nqboard.quanta.controller;

import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.security.annotation.Inner;
import com.mx.nqboard.quanta.service.TradeCalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 交易日历 内部接口
 * </p>
 * <p>
 * 仅供 Quartz 定时任务 Feign 调用（同步日历 / 开盘闸门），不提供前端 CRUD 能力
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tradeCal")
public class TradeCalController {

	private final TradeCalService tradeCalService;

	/**
	 * 系统内部接口：从 tushare 同步交易日历，供 RemoteTradeCalService Feign 调用（Quartz 定时任务），故使用 {@code @Inner} 免鉴权
	 *
	 * @return 同步处理的行数
	 */
	@Inner
	@PostMapping("/sync")
	public R<Integer> syncFromTushare() {
		return R.ok(tradeCalService.syncFromTushare());
	}

	/**
	 * 系统内部接口：今天是否开盘（定时任务执行前的交易日闸门），供 RemoteTradeCalService Feign 调用，故使用 {@code @Inner} 免鉴权
	 *
	 * @return true=开盘（交易日），false=休市
	 */
	@Inner
	@PostMapping("/isOpenToday")
	public R<Boolean> isOpenToday() {
		return R.ok(tradeCalService.isOpenToday());
	}

}
