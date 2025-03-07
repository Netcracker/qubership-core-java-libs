package org.qubership.cloud.bluegreen.impl.http.error;

public class InvocationException extends RuntimeException {
    public InvocationException(Throwable cause) {
        super(cause);
    }
}
