package com.netcracker.cloud.junit.cloudcore.extension.client;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public interface Fabric8KubernetesClientBuilderAdapter {

    KubernetesClientBuilder adapt(KubernetesClientBuilder clientBuilder);
}
