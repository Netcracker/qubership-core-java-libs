package com.netcracker.cloud.junit.cloudcore.extension.provider;

import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface CloudCoreResourceFactory {

    KubernetesClient getKubernetesClient(String cloud, String namespace);

    PortForwardService getPortForwardService(PortForwardConfig config);

    void close();

}
