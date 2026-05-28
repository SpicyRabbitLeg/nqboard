package com.mx.workflow.controller;

import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.security.annotation.HasPermission;
import com.mx.workflow.service.FlowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 工作流流程实例管理 前端控制器
 * </p>
 *
 * @author 泥鳅压滑板
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flowInstance")
@Tag(description = "flowInstance", name = "工作流流程实例管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FlowInstanceController {

    private final FlowInstanceService flowInstanceService;


    /**
     * 删除流程实例
     *
     * @param ids  ids
     * @return R
     */
    @Operation(summary = "删除流程实例", description = "删除流程实例")
    @DeleteMapping
    @HasPermission("workflow_flowInstance_del")
    public R delete(@RequestBody List<String> ids) {
        for (String instanceId : ids) {
            flowInstanceService.delete(instanceId);
        }
        return R.ok();
    }
}
