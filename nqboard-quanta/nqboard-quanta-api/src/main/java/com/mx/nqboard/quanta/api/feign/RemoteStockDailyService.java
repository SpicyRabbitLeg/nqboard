package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 股票日线行情 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockDailyService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockDailyService {

	/**
	 * 从 tushare 同步股票日线行情（按市场过滤，支持全量/增量）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param market 市场类型：主板/创业板/科创板，为空时取 yml 配置 tushare.daily.market
	 * @param full 是否全量同步：true=2024-01-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockDaily/sync")
	R<Integer> syncFromTushare(@RequestParam(value = "market", required = false) String market,
			@RequestParam(value = "full", required = false) Boolean full);

}
