package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class PortForwardParams {
    private final String serviceName;
    private final int port;
    private String namespace;

    public PortForwardParams withNamespace(String namespace) {
        Objects.requireNonNull(namespace);
        this.namespace = namespace;
        return this;
    }
}
