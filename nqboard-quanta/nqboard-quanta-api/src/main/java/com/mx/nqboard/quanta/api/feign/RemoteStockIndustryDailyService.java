package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 行业板块日线 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockIndustryDailyService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockIndustryDailyService {

	/**
	 * 从 东方财富 同步行业板块日线K线（先拉板块列表，再逐板块拉K线）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param full 是否全量同步：true=从2026-01-01起；false=仅今天；为空时取 yml 配置
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockIndustryDaily/sync")
	R<Integer> syncFromEastMoney(@RequestParam(value = "full", required = false) Boolean full);

}
