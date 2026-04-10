package com.mx.nqboard.common.core.exception;

import lombok.NoArgsConstructor;

/**
 * @author 泥鳅压滑板
 */
@NoArgsConstructor
public class WritePointException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WritePointException(String message) {
        super(message);
    }

    public WritePointException(Throwable cause) {
        super(cause);
    }

    public WritePointException(String message, Throwable cause) {
        super(message, cause);
    }

    public WritePointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
