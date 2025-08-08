package com.netcracker.cloud.junit.cloudcore.extension.callbacks;

import org.qubership.cloud.junit.cloudcore.extension.annotations.Cloud;
import org.qubership.cloud.junit.cloudcore.extension.annotations.IntValue;
import org.qubership.cloud.junit.cloudcore.extension.annotations.PortForward;
import org.qubership.cloud.junit.cloudcore.extension.annotations.Value;
import org.qubership.cloud.junit.cloudcore.extension.provider.CloudCoreResourceFactory;
import org.qubership.cloud.junit.cloudcore.extension.provider.CloudCoreResourceFactoryProvider;
import org.qubership.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import org.qubership.cloud.junit.cloudcore.extension.provider.PortForwardConfig;
import org.qubership.cloud.junit.cloudcore.extension.service.Endpoint;
import org.qubership.cloud.junit.cloudcore.extension.service.NetSocketAddress;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardParams;
import org.qubership.cloud.junit.cloudcore.extension.service.PortForwardService;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

public class CloudCoreJunitCallback implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LocalHostAddressGenerator.setup();
        CloudCoreResourceFactory cloudCoreResourceFactory = CloudCoreResourceFactoryProvider.getFactory(context);
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();
        while (clazz != Object.class) {
            processFieldsBeforeAllClass(context, cloudCoreResourceFactory, testInstance, clazz);
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        CloudCoreResourceFactory cloudCoreResourceFactory = CloudCoreResourceFactoryProvider.getFactory(context);
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();
        Class<?> enclosingClass = clazz.getEnclosingClass();
        while (clazz != Object.class) {
            processFieldsAfterAllClass(cloudCoreResourceFactory, testInstance, clazz);
            clazz = clazz.getSuperclass();
        }
        if (enclosingClass == null) {
            // as the last step for non-inner test classes, close the rest port-forwards which might be created manually
            cloudCoreResourceFactory.close();
        }
    }

    private void processFieldsBeforeAllClass(ExtensionContext context, CloudCoreResourceFactory cloudCoreResourceFactory, Object testInstance, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            processFieldBeforeAll(context, cloudCoreResourceFactory, testInstance, field);
        }
    }

    private void processFieldsAfterAllClass(CloudCoreResourceFactory cloudCoreResourceFactory, Object testInstance, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            processFieldAfterAll(cloudCoreResourceFactory, testInstance, field);
        }
    }

    protected void processFieldBeforeAll(ExtensionContext context, CloudCoreResourceFactory resourceFactory, Object testInstance, Field field) throws Exception {
        Class<?> type = field.getType();
        if (KubernetesClient.class.isAssignableFrom(type)) {
            String cloud = resolveValue(testInstance, field, Cloud.class, Cloud::cloud);
            String namespace = resolveValue(testInstance, field, Cloud.class, Cloud::namespace);
            setValueToField(field, testInstance, resourceFactory.getKubernetesClient(cloud, namespace));
        } else if (field.getAnnotation(PortForward.class) != null &&
                   (URL.class.isAssignableFrom(type) || URI.class.isAssignableFrom(type) || NetSocketAddress.class.isAssignableFrom(type))) {
            String cloud = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().cloud());
            String namespace = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().namespace());
            NetSocketAddress address = createPortForward(resourceFactory, testInstance, field, cloud, namespace);
            Object fieldValue;
            if (NetSocketAddress.class.isAssignableFrom(type)) {
                fieldValue = address;
            } else {
                var protocol = resolveValue(testInstance, field, PortForward.class, PortForward::protocol);
                URI uri = buildUri(protocol, address);
                if (URI.class.isAssignableFrom(type)) {
                    fieldValue = uri;
                } else {
                    fieldValue = uri.toURL();
                }
            }
            setValueToField(field, testInstance, fieldValue);
        } else if (PortForwardService.class.isAssignableFrom(type)) {
            String cloud = resolveValue(testInstance, field, Cloud.class, Cloud::cloud);
            String namespace = resolveValue(testInstance, field, Cloud.class, Cloud::namespace);
            PortForwardService portForwardService = resourceFactory.getPortForwardService(new PortForwardConfig(cloud, namespace));
            setValueToField(field, testInstance, portForwardService);
        }
    }

    protected static NetSocketAddress createPortForward(CloudCoreResourceFactory resourceFactory, Object testInstance, Field field, String cloud, String namespace) throws IllegalArgumentException {
        PortForward portForwardAnn = field.getAnnotation(PortForward.class);
        if (portForwardAnn == null)
            throw new IllegalArgumentException(String.format("@PortForward annotation is required on field: '%s' in class '%s'",
                    field.getName(), testInstance.getClass().getName()));
        PortForwardService portForwardService = resourceFactory.getPortForwardService(new PortForwardConfig(cloud, namespace));
        String serviceName = resolveValue(testInstance, field, PortForward.class, PortForward::serviceName);
        int port = resolveIntValue(testInstance, field, PortForward.class, PortForward::port);
        return portForwardService.portForward(new PortForwardParams(serviceName, port));
    }

    private void processFieldAfterAll(CloudCoreResourceFactory resourceFactory, Object testInstance, Field field) throws Exception {
        processUrlCleanup(resourceFactory, testInstance, field);
    }

    private void processUrlCleanup(CloudCoreResourceFactory cloudCoreResourceFactory, Object testInstance, Field field) throws Exception {
        PortForward portForwardAnn = field.getAnnotation(PortForward.class);
        if (portForwardAnn != null) {
            String cloud = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().cloud());
            String namespace = resolveValue(testInstance, field, PortForward.class, pf -> pf.cloud().namespace());
            PortForwardConfig config = new PortForwardConfig(cloud, namespace);
            PortForwardService portForwardService = cloudCoreResourceFactory.getPortForwardService(config);
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
            if (address != null)  portForwardService.closePortForward(new Endpoint(address.getHostString(), address.getPort()));
        }
    }

    protected static @NotNull URI buildUri(String protocol, NetSocketAddress address) {
        return URI.create(String.format("%s://%s:%d/", protocol, address.getHostName(), address.getPort()));
    }

    protected static <T extends Annotation> String resolveValue(Object testInstance, Field field, Class<T> annClass,
                                                              Function<T, Value> valueExtractor) throws IllegalArgumentException {
        return Optional.ofNullable(field.getAnnotation(annClass)).map(ann -> {
            Value v = valueExtractor.apply(ann);
            if (!v.value().isBlank()) {
                return v.value();
            } else if (!v.prop().isBlank()) {
                String resolved = System.getProperty(v.prop());
                if (resolved == null) {
                    throw new IllegalArgumentException(String.format("@%s annotation's at field '%s' in class '%s' is invalid - prop: '%s' not found",
                            annClass.getSimpleName(), field.getName(), testInstance.getClass().getName(), v.prop()));
                }
                return resolved;
            } else {
                return null;
            }
        }).orElseThrow(() -> new IllegalArgumentException(String.format("@%s annotation with valid parameters is required on field: '%s' in class '%s'",
                annClass.getSimpleName(), field.getName(), testInstance.getClass().getName())));
    }

    private static <T extends Annotation> int resolveIntValue(Object testInstance, Field field, Class<T> annClass,
                                                              Function<T, IntValue> valueExtractor) throws IllegalArgumentException {
        return Optional.ofNullable(field.getAnnotation(annClass)).map(ann -> {
            IntValue v = valueExtractor.apply(ann);
            if (v.value() != -1) {
                return v.value();
            } else if (!v.prop().isBlank()) {
                String resolved = System.getProperty(v.prop());
                if (resolved == null) {
                    throw new IllegalArgumentException(String.format("@%s annotation's at field '%s' in class '%s' is invalid - prop: '%s' not found",
                            annClass.getSimpleName(), field.getName(), testInstance.getClass().getName(), v.prop()));
                }
                return Integer.valueOf(resolved);
            } else {
                return null;
            }
        }).orElseThrow(() -> new IllegalArgumentException(String.format("@%s annotation with valid parameters is required on field: '%s' in class '%s'",
                annClass.getSimpleName(), field.getName(), testInstance.getClass().getName())));
    }

    protected void setValueToField(Field field, Object testInstance, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(testInstance, value);
    }

    protected <T> T getValueFromField(Field field, Object testInstance, Class<T> clazz) throws IllegalAccessException {
        field.setAccessible(true);
        return clazz.cast(field.get(testInstance));
    }
}
