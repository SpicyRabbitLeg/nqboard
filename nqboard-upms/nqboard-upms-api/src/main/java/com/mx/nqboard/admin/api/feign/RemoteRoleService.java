package com.mx.nqboard.admin.api.feign;

import com.mx.nqboard.admin.api.entity.SysRole;
import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @author 泥鳅压滑板
 */
@FeignClient(contextId = "remoteRoleService", value = ServiceNameConstants.UPMS_SERVICE)
public interface RemoteRoleService {

    /**
     * 查询角色信息
     *
     * @param query 查询条件
     * @return 角色信息
     */
    @GetMapping("/role/detailsList")
	R<List<SysRole>> getDetails(@SpringQueryMap SysRole query);
}
