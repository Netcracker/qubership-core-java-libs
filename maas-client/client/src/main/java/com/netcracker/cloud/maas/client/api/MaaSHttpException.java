package com.netcracker.cloud.maas.client.api;

/**
 * A call to maas-agent that did not succeed: an unexpected status code, or a transport
 * failure that outlived the retry budget. The message is taken as is, unlike
 * {@link MaaSException}, because it carries request and response text.
 */
public class MaaSHttpException extends MaaSException {

    public MaaSHttpException(String message) {
        super(message, (Throwable) null);
    }

    public MaaSHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
