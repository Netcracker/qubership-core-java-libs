package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
public class ServicePortForwardParams<T> implements BasePortForwardParams<T> {
    private final String serviceName;
    private final int port;
    private final String namespace;
    private final Class<T> resultType;
    private final String scheme;

    @Builder
    private ServicePortForwardParams(String namespace, String serviceName, int port, Class<T> resultType, String scheme) {
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.port = port;
        this.resultType = resultType;
        this.scheme = scheme;
    }

    public static ServicePortForwardParamsBuilder<NetSocketAddress> builder(String serviceName, int port) {
        return new ServicePortForwardParamsBuilder<NetSocketAddress>().serviceName(serviceName).port(port).resultType(NetSocketAddress.class);
    }

    public static ServicePortForwardParamsBuilder<URL> builderAsUrl(String serviceName, int port, String scheme) {
        return new ServicePortForwardParamsBuilder<URL>().serviceName(serviceName).port(port).resultType(URL.class).scheme(scheme);
    }

    @Override
    public String getName() {
        return serviceName;
    }

    @Override
    public String host(String namespace) {
        return String.format("%s.%s", serviceName, namespace);
    }

    @Override
    public T supply(NetSocketAddress address) {
        if (resultType.isAssignableFrom(NetSocketAddress.class)) {
            return resultType.cast(address);
        } else if (resultType.isAssignableFrom(URL.class)) {
            return resultType.cast(address.toUrl(scheme));
        } else {
            throw new IllegalArgumentException("Unsupported result type: " + resultType.getName());
        }
    }
}
