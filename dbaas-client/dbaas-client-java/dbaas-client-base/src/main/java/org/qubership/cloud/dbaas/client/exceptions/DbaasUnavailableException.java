package org.qubership.cloud.dbaas.client.exceptions;

public class DbaasUnavailableException extends RuntimeException {
    public DbaasUnavailableException() {
    }

    public DbaasUnavailableException(String message) {
        super(message);
    }

    public DbaasUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbaasUnavailableException(Throwable cause) {
        super(cause);
    }

    public DbaasUnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
