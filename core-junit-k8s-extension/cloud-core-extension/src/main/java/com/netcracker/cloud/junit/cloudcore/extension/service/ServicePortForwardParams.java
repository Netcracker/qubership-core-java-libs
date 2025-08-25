package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ServicePortForwardParams implements BasePortForwardParams {
    private final String serviceName;
    private final int port;
    private final String namespace;

    @Builder
    private ServicePortForwardParams(String namespace, String serviceName, int port) {
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.port = port;
    }

    public static ServicePortForwardParamsBuilder builder(String podName, int port) {
        return new ServicePortForwardParamsBuilder().serviceName(podName).port(port);
    }

    @Override
    public String getName() {
        return serviceName;
    }
}
