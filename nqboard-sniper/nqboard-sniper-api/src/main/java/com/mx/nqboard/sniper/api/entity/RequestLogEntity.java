package com.mx.nqboard.sniper.api.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 外部请求审计（Python CN_REQUEST_LOG 等价物，对拍 fixtures 录制源；append-only，
 * 审计列由 AOP 写入路径显式赋值不依赖填充器，del_flag 恒 '0' 不参与过滤）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_request_log")
@Schema(description = "外部请求审计")
@EqualsAndHashCode(callSuper = true)
public class RequestLogEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 雪花id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "雪花id")
	private Long id;

	/**
	 * 删除状态（恒 '0'，append-only 不参与过滤）
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除状态（0未删除、1删除）")
	private String delFlag;

	/**
	 * 调用时间
	 */
	@Schema(description = "调用时间")
	private LocalDateTime calledAt;

	/**
	 * 端点标识
	 */
	@Schema(description = "端点标识")
	private String endpoint;

	/**
	 * 数据源 tushare/em/tencent/sina/dify
	 */
	@Schema(description = "数据源 tushare/em/tencent/sina/dify")
	private String source;

	/**
	 * 请求参数 JSON
	 */
	@Schema(description = "请求参数 JSON")
	private String params;

	/**
	 * 调用状态 ok/error
	 */
	@Schema(description = "调用状态 ok/error")
	private String status;

	/**
	 * 返回行数
	 */
	@Schema(description = "返回行数")
	private Integer nRows;

	/**
	 * 耗时（毫秒）
	 */
	@Schema(description = "耗时（毫秒）")
	private Integer elapsedMs;

	/**
	 * 错误摘要（≤500 字）
	 */
	@Schema(description = "错误摘要（≤500 字）")
	private String error;

}
