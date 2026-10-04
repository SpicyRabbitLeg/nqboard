package com.mx.nqboard.sniper.data.provider.tushare;

/**
 * <p>
 * Tushare 调用异常（HTTP 失败 / 信封 code!=0 / 响应非法）。
 * 异常 message 是限频退避判定的输入（关键词照 Python {@code _is_rate_limit_error}），勿随意改写格式。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public class TushareClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TushareClientException(String message) {
		super(message);
	}

	public TushareClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
