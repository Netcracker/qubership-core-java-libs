package org.qubership.cloud.junit.cloudcore.extension.provider;

import org.qubership.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CloudCoreResourceFactoryProvider {
    public static ExtensionContext.Namespace CloudCoreNamespace = ExtensionContext.Namespace.create("qubership.cloud-core");

    public static CloudCoreResourceFactory getFactory(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(CloudCoreNamespace);
        return store.getOrComputeIfAbsent(CloudCoreResourceFactory.class, key ->
                new CloudCoreResourceFactoryImpl(new KubernetesClientFactory(new ConfigBuilder().build())), CloudCoreResourceFactory.class);
    }
}
