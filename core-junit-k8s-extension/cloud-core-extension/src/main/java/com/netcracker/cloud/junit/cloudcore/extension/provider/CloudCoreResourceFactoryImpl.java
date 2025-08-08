package com.netcracker.cloud.junit.cloudcore.extension.provider;

import org.qubership.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CloudCoreResourceFactoryImpl implements CloudCoreResourceFactory {

    protected KubernetesClientFactory kubernetesClientFactory;
    protected Map<PortForwardConfig, PortForwardService> portForwardServiceMap = new ConcurrentHashMap<>();


    public CloudCoreResourceFactoryImpl(KubernetesClientFactory kubernetesClientFactory) {
        this.kubernetesClientFactory = kubernetesClientFactory;
    }

    @Override
    public KubernetesClient getKubernetesClient(String cloud, String namespace) {
        return kubernetesClientFactory.getKubernetesClient(cloud, namespace);
    }

    @Override
    public PortForwardService getPortForwardService(PortForwardConfig config) {
        return this.portForwardServiceMap.computeIfAbsent(config, c ->
                new PortForwardService(getKubernetesClient(config.getCloud(), config.getNamespace())));
    }

    @Override
    public void close() {
        this.portForwardServiceMap.values().stream().filter(Objects::nonNull).forEach(PortForwardService::closePortForwards);
    }
}
