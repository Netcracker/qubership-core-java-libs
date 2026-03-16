package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Cloud;
import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.lang.reflect.Field;

import static com.netcracker.cloud.junit.cloudcore.extension.callbacks.CloudCoreJunitCallback.resolveValue;

public class KubernetesClientProvider implements FieldInstanceProvider {

    @Override
    public KubernetesClient createInstance(Object testInstance, Field field) {
        KubernetesClientFactory kubernetesClientFactory = OrderedServiceLoader.load(KubernetesClientFactory.class)
                .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));
        String cloud = resolveValue(testInstance, field, Cloud.class, Cloud::cloud, kubernetesClientFactory.getCurrentContext());
        String namespace = resolveValue(testInstance, field, Cloud.class, Cloud::namespace, kubernetesClientFactory.getNamespace());
        return kubernetesClientFactory.getKubernetesClient(cloud, namespace);
    }

    @Override
    public void destroyInstance(Object testInstance, Field field) {

    }

    @Override
    public boolean test(Field field) {
        return KubernetesClient.class.isAssignableFrom(field.getType());
    }
}
