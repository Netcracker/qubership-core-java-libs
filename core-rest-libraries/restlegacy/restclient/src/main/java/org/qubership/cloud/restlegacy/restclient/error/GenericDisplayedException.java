package org.qubership.cloud.restlegacy.restclient.error;

/**
 * Simple exception class that supports {@link DisplayedMessageException}
 */
@DisplayedMessageException
/**
 * Exception can be used for exception handling version 1, 2
 * For exception version 2.1 use  {@link org.qubership.cloud.microserviceframework.error.v2.v2_1.GenericDisplayedException}
 */
public class GenericDisplayedException extends RuntimeException {
    public GenericDisplayedException(String message) {
        super(message);
    }

    public GenericDisplayedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericDisplayedException(Throwable cause) {
        super(cause);
    }
}
