package com.netcracker.cloud.maas.client.api;

/** A call to maas that did not succeed: an unexpected status code or a transport failure. */
public class MaaSHttpException extends MaaSException {

    public MaaSHttpException(String message) {
        super(message, (Throwable) null);
    }

    public MaaSHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
