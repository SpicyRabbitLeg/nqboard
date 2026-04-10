package com.mx.nqboard.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author 泥鳅压滑板
 **/
@Data
public class UploadFileDTO {

	/**
	 * 上传的文件资源
	 */
	@NotEmpty(message = "上传的文件资源不能为空")
	@Schema(description = "上传的文件资源")
	private String base64Data;

	/**
	 * 文件名 如：index.png
	 */
	@NotEmpty(message = "文件名 不能为空")
	@Schema(description = "文件名 如：index.png",example = "index.gif")
	private String fileName;

	/**
	 * 文件类型 如：image/gif
	 */
	@NotEmpty(message = "文件类型不能为空")
	@Pattern(regexp = "^(image|application|text)/(jpeg|png|gif|bmp|webp|pdf|doc|docx|xls|xlsx|txt|mp4|mp3|octet-stream)$",
			message = "文件类型不合法，请传入标准MIME类型（如 image/png、image/gif、application/pdf 等）")
	@Schema(description = "文件类型 如：image/gif",example = "image/gif")
	private String fileType;
}
