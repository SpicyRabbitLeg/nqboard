package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 龙虎榜每日明细 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockTopListService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockTopListService {

	/**
	 * 从 tushare 同步龙虎榜每日明细（按日期遍历，支持全量/增量/指定日期）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param tradeDate 指定交易日期 YYYYMMDD（可空；为空时按 full 决定范围）
	 * @param full 是否全量同步：true=2024-01-01 至今天；false=仅增量获取今天；为空时取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockTopList/sync")
	R<Integer> syncFromTushare(@RequestParam(value = "tradeDate", required = false) String tradeDate,
			@RequestParam(value = "full", required = false) Boolean full);

}
