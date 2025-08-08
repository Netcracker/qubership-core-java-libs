package org.qubership.cloud.junit.cloudcore.extension.service;

public record Endpoint(String host, int port) {

    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }
}
