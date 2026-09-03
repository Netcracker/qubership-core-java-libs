package com.netcracker.cloud.maas.client.api;

/** A call to maas that did not succeed: an unexpected status code or a transport failure. */
public class MaaSHttpException extends MaaSException {

    public static MaaSHttpException of(String message) {
        return new MaaSHttpException(message, null);
    }

    public MaaSHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
