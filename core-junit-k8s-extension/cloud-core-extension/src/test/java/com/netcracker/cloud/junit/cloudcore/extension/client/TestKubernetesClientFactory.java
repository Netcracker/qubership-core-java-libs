package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.provider.CloudAndNamespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Setter;

import java.util.function.Function;

public class TestKubernetesClientFactory implements AutoCloseable, KubernetesClientFactory {

    @Setter
    private static Function<CloudAndNamespace, KubernetesClient> function;

    public KubernetesClient getKubernetesClient(String context, String namespace) {
        if (function == null) {
            throw new IllegalStateException("No function provided");
        }
        return function.apply(new CloudAndNamespace(context, namespace));
    }

    @Override
    public void close() {
    }
}
