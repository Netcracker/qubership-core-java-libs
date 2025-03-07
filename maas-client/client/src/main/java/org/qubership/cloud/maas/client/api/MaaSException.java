package org.qubership.cloud.maas.client.api;

public class MaaSException extends RuntimeException {
    public MaaSException(String format, Object...args) {
        super(String.format(format, args));
    }
}
