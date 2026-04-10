package com.mx.nqboard.common.core.exception;

import lombok.NoArgsConstructor;

/**
 * @author 泥鳅压滑板
 */
@NoArgsConstructor
public class FlowErrorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FlowErrorException(String message) {
        super(message);
    }

    public FlowErrorException(Throwable cause) {
        super(cause);
    }

    public FlowErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public FlowErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
