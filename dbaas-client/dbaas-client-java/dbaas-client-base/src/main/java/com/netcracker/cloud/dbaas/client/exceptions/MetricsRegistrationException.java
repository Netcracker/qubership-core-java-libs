package com.netcracker.cloud.dbaas.client.exceptions;

public class MetricsRegistrationException extends Exception {
    public MetricsRegistrationException(String message) {
        super(message);
    }

    public MetricsRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
