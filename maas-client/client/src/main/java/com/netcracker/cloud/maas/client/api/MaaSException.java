package com.netcracker.cloud.maas.client.api;

public class MaaSException extends RuntimeException {

    /**
     * Formats the message. Note that a two-argument call whose second argument is a
     * {@code Throwable} binds to the constructor below instead, and is not formatted.
     */
    public MaaSException(String format, Object...args) {
        super(String.format(format, args));
    }

    /** For subclasses whose message is already built and must not go through String.format. */
    protected MaaSException(String message, Throwable cause) {
        super(message, cause);
    }
}
