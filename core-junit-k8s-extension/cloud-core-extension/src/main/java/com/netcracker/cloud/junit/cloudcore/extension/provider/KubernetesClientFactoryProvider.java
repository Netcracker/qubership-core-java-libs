package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;

import java.lang.reflect.Field;

public class KubernetesClientFactoryProvider implements FieldInstanceProvider {

    @Override
    public KubernetesClientFactory createInstance(Object testInstance, Field field) {
        return OrderedServiceLoader.load(KubernetesClientFactory.class)
                .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));
    }

    @Override
    public void destroyInstance(Object testInstance, Field field) {

    }

    @Override
    public boolean test(Field field) {
        return KubernetesClientFactory.class.isAssignableFrom(field.getType());
    }
}
