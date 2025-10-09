package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Cloud;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.PortForward;
import com.netcracker.cloud.junit.cloudcore.extension.client.KubernetesClientFactory;
import com.netcracker.cloud.junit.cloudcore.extension.service.Endpoint;
import com.netcracker.cloud.junit.cloudcore.extension.service.NetSocketAddress;
import com.netcracker.cloud.junit.cloudcore.extension.service.PortForwardService;
import com.netcracker.cloud.junit.cloudcore.extension.service.ServicePortForwardParams;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ServiceLoader;

import static com.netcracker.cloud.junit.cloudcore.extension.callbacks.CloudCoreJunitCallback.resolveIntValue;
import static com.netcracker.cloud.junit.cloudcore.extension.callbacks.CloudCoreJunitCallback.resolveValue;

public class PortForwardProvider implements FieldInstanceProvider {
    @Override
    public Object createInstance(Object testInstance, Field field) throws Exception {
        PortForwardServiceManager resourceFactory = ServiceLoader.load(PortForwardServiceManager.class).findFirst()
                .orElseThrow(() -> new IllegalStateException("No CloudCoreResourceFactory implementation found"));
        KubernetesClientFactory kubernetesClientFactory = ServiceLoader.load(KubernetesClientFactory.class).findFirst()
                .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));

        Class<?> type = field.getType();

        PortForwardService portForwardService;
        if (PortForwardService.class.isAssignableFrom(type)) {
            Cloud cloudAnn = field.getAnnotation(Cloud.class);
            if (cloudAnn == null)
                throw new IllegalArgumentException(String.format("@Cloud annotation is required on field: '%s' in class '%s'",
                        field.getName(), testInstance.getClass().getName()));
            String cloud = resolveValue(testInstance, field, Cloud.class, Cloud::cloud, kubernetesClientFactory.getCurrentContext());
            String namespace = resolveValue(testInstance, field, Cloud.class, Cloud::namespace, kubernetesClientFactory.getNamespace());
            portForwardService = resourceFactory.getPortForwardService(new PortForwardConfig(cloud, namespace));
            return portForwardService;
        } else {
            PortForward portForwardAnn = field.getAnnotation(PortForward.class);
            if (portForwardAnn == null)
                throw new IllegalArgumentException(String.format("@PortForward annotation is required on field: '%s' in class '%s'",
                        field.getName(), testInstance.getClass().getName()));
            String cloud = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().cloud(), kubernetesClientFactory.getCurrentContext());
            String namespace = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().namespace(), kubernetesClientFactory.getNamespace());
            portForwardService = resourceFactory.getPortForwardService(new PortForwardConfig(cloud, namespace));
            String serviceName = resolveValue(testInstance, field, PortForward.class, PortForward::serviceName);
            int port = resolveIntValue(testInstance, field, PortForward.class, PortForward::port);
            NetSocketAddress address = portForwardService.portForward(ServicePortForwardParams.builder(serviceName, port).build());

            if (NetSocketAddress.class.isAssignableFrom(type)) {
                return address;
            } else {
                var protocol = resolveValue(testInstance, field, PortForward.class, PortForward::protocol);
                URL url = address.toUrl(protocol);
                if (URI.class.isAssignableFrom(type)) {
                    return url.toURI();
                } else {
                    return url;
                }
            }
        }
    }

    @Override
    public void destroyInstance(Object testInstance, Field field) throws Exception {
        PortForward portForwardAnn = field.getAnnotation(PortForward.class);
        if (portForwardAnn != null) {
            PortForwardServiceManager resourceFactory = ServiceLoader.load(PortForwardServiceManager.class).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No CloudCoreResourceFactory implementation found"));
            KubernetesClientFactory kubernetesClientFactory = ServiceLoader.load(KubernetesClientFactory.class).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No KubernetesClientFactory implementation found"));

            String cloud = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().cloud(), kubernetesClientFactory.getCurrentContext());
            String namespace = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().namespace(), kubernetesClientFactory.getNamespace());
            PortForwardConfig config = new PortForwardConfig(cloud, namespace);
            PortForwardService portForwardService = resourceFactory.getPortForwardService(config);
            processUrlCleanupInternal(portForwardService, testInstance, field);
        }
    }

    protected void processUrlCleanupInternal(PortForwardService portForwardService, Object testInstance, Field field) throws Exception {
        if (URI.class.isAssignableFrom(field.getType())) {
            URI uri = getValueFromField(field, testInstance, URI.class);
            if (uri != null) portForwardService.closePortForward(new Endpoint(uri.getHost(), uri.getPort()));
        } else if (URL.class.isAssignableFrom(field.getType())) {
            URL url = getValueFromField(field, testInstance, URL.class);
            if (url != null) portForwardService.closePortForward(new Endpoint(url.getHost(), url.getPort()));
        } else if (NetSocketAddress.class.isAssignableFrom(field.getType())) {
            NetSocketAddress address = getValueFromField(field, testInstance, NetSocketAddress.class);
            if (address != null)
                portForwardService.closePortForward(new Endpoint(address.getHostString(), address.getPort()));
        }
    }

    protected <T> T getValueFromField(Field field, Object testInstance, Class<T> clazz) throws IllegalAccessException {
        field.setAccessible(true);
        return clazz.cast(field.get(testInstance));
    }

    @Override
    public boolean test(Field field) {
        Class<?> type = field.getType();
        return PortForwardService.class.isAssignableFrom(type) ||
               (field.getAnnotation(PortForward.class) != null &&
                (URL.class.isAssignableFrom(type) || URI.class.isAssignableFrom(type) || NetSocketAddress.class.isAssignableFrom(type)));
    }
}
