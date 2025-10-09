package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PodPortForwardParams implements BasePortForwardParams {
    private final String podName;
    private final int port;
    private final String namespace;

    @Builder
    private PodPortForwardParams(String namespace, String podName, int port) {
        this.namespace = namespace;
        this.podName = podName;
        this.port = port;
    }

    public static PodPortForwardParamsBuilder builder(String podName, int port) {
        return new PodPortForwardParamsBuilder().podName(podName).port(port);
    }

    @Override
    public String getName() {
        return podName;
    }

    @Override
    public String host() {
        return String.format("%s.%s", podName, namespace);
    }
}
