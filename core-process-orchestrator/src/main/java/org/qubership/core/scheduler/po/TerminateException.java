package org.qubership.core.scheduler.po;

@SuppressWarnings("unused")
public class TerminateException extends RuntimeException{

    public TerminateException() {
        super();
    }

    public TerminateException(String message) {
        super(message);
    }

    public TerminateException(String message, Throwable cause) {
        super(message, cause);
    }

    public TerminateException(Throwable cause) {
        super(cause);
    }

    public TerminateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
