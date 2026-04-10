package com.mx.nqboard.admin.api.feign;

import com.mx.nqboard.admin.api.entity.SysDictItem;
import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 远程字典服务接口
 *
 * @author lengleng
 * @date 2025/05/30
 */
@FeignClient(contextId = "remoteDictService", value = ServiceNameConstants.UPMS_SERVICE)
public interface RemoteDictService {

	/**
	 * 通过字典类型查找字典
	 * @param type 字典类型
	 * @return 同类型字典
	 */
	@NoToken
	@GetMapping("/dict/remote/type/{type}")
	R<List<SysDictItem>> getDictByType(@PathVariable("type") String type);

}
