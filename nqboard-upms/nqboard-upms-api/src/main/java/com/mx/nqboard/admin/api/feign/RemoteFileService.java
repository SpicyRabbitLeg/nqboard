package com.mx.nqboard.admin.api.feign;

import com.mx.nqboard.admin.api.dto.UploadFileDTO;
import com.mx.nqboard.common.core.constant.ServiceNameConstants;
import com.mx.nqboard.common.core.util.R;
import com.mx.nqboard.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 远程字典文件接口
 *
 * @author 泥鳅压滑板
 */
@FeignClient(contextId = "remoteFileService", value = ServiceNameConstants.UPMS_SERVICE)
public interface RemoteFileService {

	/**
	 * 上传文件base64
	 *
	 * @param uploadFileDto 上传的文件资源
	 * @return 包含文件路径的R对象，格式为(/admin/bucketName/filename)
	 */
	@NoToken
	@PostMapping(value = "/sys-file/uploadBase64")
	R<Map<String, String>> uploadBase64(@Validated @RequestBody UploadFileDTO uploadFileDto);
}
