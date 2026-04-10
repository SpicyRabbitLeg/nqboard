package com.mx.nqboard.common.core.exception;

import lombok.NoArgsConstructor;

/**
 *
 * @author 泥鳅压滑板
 */
@NoArgsConstructor
public class ReadPointException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReadPointException(String message) {
        super(message);
    }

    public ReadPointException(Throwable cause) {
        super(cause);
    }

    public ReadPointException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadPointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
