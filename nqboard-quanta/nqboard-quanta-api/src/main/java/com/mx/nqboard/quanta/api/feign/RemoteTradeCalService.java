package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * <p>
 * 交易日历 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteTradeCalService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteTradeCalService {

	/**
	 * 从 tushare 同步交易日历
	 * <p>
	 * 仅供系统内部调用（Quartz 定时任务每日开盘前刷新日历），配合 provider 端 {@code @Inner} 放行
	 * </p>
	 *
	 * @return 同步处理的行数
	 */
	@NoToken
	@PostMapping("/tradeCal/sync")
	R<Integer> syncFromTushare();

	/**
	 * 今天是否开盘（Quartz 定时任务执行前的交易日闸门）
	 * <p>
	 * 仅供系统内部调用，配合 provider 端 {@code @Inner} 放行
	 * </p>
	 *
	 * @return true=开盘（交易日），false=休市
	 */
	@NoToken
	@PostMapping("/tradeCal/isOpenToday")
	R<Boolean> isOpenToday();

}
