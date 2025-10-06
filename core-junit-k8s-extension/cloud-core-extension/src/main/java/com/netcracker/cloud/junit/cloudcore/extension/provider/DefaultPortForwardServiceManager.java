package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultPortForwardServiceManager implements PortForwardServiceManager {

    protected static Map<PortForwardConfig, PortForwardService> portForwardServiceMap = new ConcurrentHashMap<>();

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        return portForwardServiceMap.computeIfAbsent(config, c -> {
            KubernetesClientFactory kubernetesClientFactory = ServiceLoader.load(KubernetesClientFactory.class).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));
            KubernetesClient kubernetesClient = kubernetesClientFactory.getKubernetesClient(c.getCloud(), c.getNamespace());
            return new PortForwardService(kubernetesClient);
        });
    }

    @Override
    public void close() {
        portForwardServiceMap.values().stream().filter(Objects::nonNull).forEach(PortForwardService::closePortForwards);
    }
}
