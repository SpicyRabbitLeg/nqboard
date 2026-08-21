package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 每日筛选打分 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockScreenResultService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockScreenResultService {

	/**
	 * 触发筛选打分（以指数日线最新交易日为信号日）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param date 信号日 YYYYMMDD，为空时自动取最新交易日
	 * @return 处理的股票数
	 */
	@NoToken
	@PostMapping("/stockScreenResult/screen")
	R<Integer> screen(@RequestParam(value = "date", required = false) String date);

}
