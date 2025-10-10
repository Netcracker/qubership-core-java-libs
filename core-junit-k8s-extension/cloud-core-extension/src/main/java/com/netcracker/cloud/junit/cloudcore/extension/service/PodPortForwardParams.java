package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
public class PodPortForwardParams<T> implements BasePortForwardParams<T> {
    private final String podName;
    private final int port;
    private final String namespace;
    private final Class<T> resultType;
    private final String scheme;

    @Builder
    private PodPortForwardParams(String namespace, String podName, int port, Class<T> resultType, String scheme) {
        this.namespace = namespace;
        this.podName = podName;
        this.port = port;
        this.resultType = resultType;
        this.scheme = scheme;
    }

    public static PodPortForwardParamsBuilder<NetSocketAddress> builder(String podName, int port) {
        return new PodPortForwardParamsBuilder<NetSocketAddress>().podName(podName).port(port).resultType(NetSocketAddress.class);
    }

    public static PodPortForwardParamsBuilder<URL> builderAsUrl(String podName, int port, String scheme) {
        return new PodPortForwardParamsBuilder<URL>().podName(podName).port(port).resultType(URL.class).scheme(scheme);
    }

    @Override
    public String getName() {
        return podName;
    }

    @Override
    public String host(String namespace) {
        return String.format("%s.%s", podName, namespace);
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
