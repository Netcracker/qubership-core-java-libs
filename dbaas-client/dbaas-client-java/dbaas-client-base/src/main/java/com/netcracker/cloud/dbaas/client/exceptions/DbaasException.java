package org.qubership.cloud.dbaas.client.exceptions;

public class DbaasException extends RuntimeException {
    public DbaasException() {
    }

    public DbaasException(String message) {
        super(message);
    }

    public DbaasException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbaasException(Throwable cause) {
        super(cause);
    }

    public DbaasException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
