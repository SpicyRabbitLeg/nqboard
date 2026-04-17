package com.mx.nqboard.common.core.exception;

/**
 * 导入文件错误
 *
 * @author SpicyRabbitLeg
 */
public class ImportFileException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ImportFileException(String message) {
        super(message);
    }

    public ImportFileException(Throwable cause) {
        super(cause);
    }

    public ImportFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImportFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
