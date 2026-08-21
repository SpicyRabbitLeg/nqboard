package com.mx.nqboard.quanta.api.feign;

import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 盘后数据流水线 远程调用接口
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@FeignClient(contextId = "remoteQuantPipelineService", value = ServiceNameConstants.QUANTA_SERVICE)
public interface RemoteQuantPipelineService {

	/**
	 * 触发盘后流水线（异步执行，立即返回运行id）
	 * <p>
	 * 仅供系统内部调用（如 Quartz 定时任务），发起方无需带 token，配合 provider 端 {@code @Inner} 放行
	 * @return 本次运行的 runId
	 */
	@NoToken
	@PostMapping("/quantPipeline/run")
	R<String> runPipeline();

	/**
	 * 重跑流水线中的单个步骤（异步执行，立即返回运行id）
	 * @param step 步骤编码（见 QuantPipelineService 步骤定义）
	 * @return 本次运行的 runId
	 */
	@NoToken
	@PostMapping("/quantPipeline/runStep")
	R<String> runStep(@RequestParam("step") String step);

}
