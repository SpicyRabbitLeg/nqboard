package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 指数成分股及权重 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteStockConsWeightService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteStockConsWeightService {

	/**
	 * 从 中证指数官网 同步指数成分股权重
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @param filePath 本地 xls 文件路径（可空；为空则按 yml 配置的指数列表从官网下载）
	 * @return 同步成功的条数
	 */
	@NoToken
	@PostMapping("/stockConsWeight/sync")
	R<Integer> syncFromCsindex(@RequestParam(value = "filePath", required = false) String filePath);

}
