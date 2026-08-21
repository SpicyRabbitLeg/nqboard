package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 个股主力资金流 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockMoneyFlowService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockMoneyFlowService {

	/**
	 * 从 东方财富 同步个股主力资金流（当日全市场快照）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockMoneyFlow/sync")
	R<Integer> syncFromEastMoney(@RequestParam(value = "full", required = false) Boolean full);

}
