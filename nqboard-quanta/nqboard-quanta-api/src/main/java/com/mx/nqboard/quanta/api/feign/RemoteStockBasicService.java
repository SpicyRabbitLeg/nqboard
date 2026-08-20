package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 股票基础信息 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockBasicService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockBasicService {

	/**
	 * 从 tushare 同步股票基础信息（按市场）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param market 市场类型：主板/创业板/科创板，为空时同步 "主板"
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockBasic/sync")
	R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market);

}
