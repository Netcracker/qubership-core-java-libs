package org.qubership.cloud.core.error.runtime;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

/**
 * Exception class with support for mandatory information like:<br>
 * 1) id (error id as UUID) - generated upon exception instantiation<br>
 * 2) errorCode - code and title of this exception<br>
 * 3) getMessage() method which prepends error code and error id<br>
 * to the detail field or to the error title (in case detail not provided)
 * <br><br>
 * also it supports optional parameters like:<br>
 * 1) detail - detailed message related to particular instance<br>
 * 2) status - http status code, if any<br>
 * 3) source - information of the source of this error, if any
 */
public class ErrorCodeException extends RuntimeException {
    @Getter
    @Setter
    @NonNull
    private String id = UUID.randomUUID().toString();
    @Getter
    private final ErrorCode errorCode;
    @Getter
    private final String detail;

    /**
     * @param errorCode error code of this exception
     * @param detail    detailed message of the exception,
     *                  includes context information related to this particular exception instance
     */
    public ErrorCodeException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    /**
     * @param errorCode error code of this exception
     * @param detail    detailed message of the exception,
     *                  includes context information related to this particular exception instance
     * @param cause     Throwable which caused this exception
     */
    public ErrorCodeException(ErrorCode errorCode, String detail, Throwable cause) {
        super(cause);
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * @param errorCode          error code of this exception
     * @param detail             detailed message of the exception,
     *                           includes context information related to this particular exception instance
     * @param cause              Throwable which caused this exception
     * @param enableSuppression  whether suppression is enabled or disabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    public ErrorCodeException(ErrorCode errorCode, String detail, Throwable cause,
                              boolean enableSuppression,
                              boolean writableStackTrace) {
        super(null, cause, enableSuppression, writableStackTrace);
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.errorCode = errorCode;
        this.detail = detail;
    }

    @Override
    public String getMessage() {
        return String.format("[%s][%s] %s",
                this.errorCode.getCode(), this.getId(),
                this.detail != null ? this.detail : this.errorCode.getTitle());
    }
}
