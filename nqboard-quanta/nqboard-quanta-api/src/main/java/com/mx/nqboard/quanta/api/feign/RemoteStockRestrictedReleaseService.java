package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 限售解禁 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockRestrictedReleaseService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockRestrictedReleaseService {

	/**
	 * 从 tushare 同步限售解禁数据（share_float）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param full 是否全量回补：true=按 yml 配置的回补天数逐日拉取；false=仅今天
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockRestrictedRelease/sync")
	R<Integer> syncFromTushare(@RequestParam(value = "full", required = false) Boolean full);

}
