package com.netcracker.cloud.junit.cloudcore.extension.client;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface KubernetesClientFactory {
    KubernetesClient getKubernetesClient(String context, String namespace);

    String getCurrentContext();

    String getNamespace();
}
