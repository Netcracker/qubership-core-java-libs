package com.netcracker.cloud.junit.cloudcore.extension.callbacks;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.IntValue;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.Value;
import com.netcracker.cloud.junit.cloudcore.extension.provider.FieldInstanceProvider;
import com.netcracker.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import com.netcracker.cloud.junit.cloudcore.extension.provider.OrderedServiceLoader;
import com.netcracker.cloud.junit.cloudcore.extension.provider.PortForwardServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

@Slf4j
public class CloudCoreJunitCallback implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LocalHostAddressGenerator.setup();
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();
        while (clazz != Object.class) {
            processFieldsBeforeAllClass(testInstance, clazz);
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> clazz = testInstance.getClass();
        Class<?> enclosingClass = clazz.getEnclosingClass();
        while (clazz != Object.class) {
            processFieldsAfterAllClass(testInstance, clazz);
            clazz = clazz.getSuperclass();
        }
        if (enclosingClass == null) {
            log.info("Closing all port-forwards in AfterAllCallback. Test class: {}", testInstance.getClass().getName());
            // as the last step for non-inner test classes, close the rest port-forwards which might be created manually
            PortForwardServiceManager portForwardServiceManager = OrderedServiceLoader.load(PortForwardServiceManager.class)
                    .orElseThrow(() -> new IllegalStateException("No PortForwardServiceManager implementation found"));
            portForwardServiceManager.close();
        }
    }

    private void processFieldsBeforeAllClass(Object testInstance, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            processFieldBeforeAll(testInstance, field);
        }
    }

    private void processFieldsAfterAllClass(Object testInstance, Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            processFieldAfterAll(testInstance, field);
        }
    }

    protected void processFieldBeforeAll(Object testInstance, Field field) throws Exception {
        Optional<FieldInstanceProvider> fieldInstanceProviderOpt = ServiceLoader.load(FieldInstanceProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(p -> p.test(field))
                .min(Comparator.comparing(FieldInstanceProvider::order));

        if (fieldInstanceProviderOpt.isEmpty()) {
            log.trace("No FieldInstanceProvider found for field: '{}' with type: '{}'", field.getName(), field.getType().getName());
            return;
        }
        try {
            Object instance = fieldInstanceProviderOpt.get().createInstance(testInstance, field);
            setValueToField(field, testInstance, instance);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to create/set instance for field '%s' of type: '%s' in '%s'",
                    field.getName(), field.getType().getName(), testInstance.getClass().getName()), e);
        }
    }

    private void processFieldAfterAll(Object testInstance, Field field) throws Exception {
        Optional<FieldInstanceProvider> fieldInstanceProviderOpt = ServiceLoader.load(FieldInstanceProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(p -> p.test(field))
                .min(Comparator.comparing(FieldInstanceProvider::order));

        if (fieldInstanceProviderOpt.isEmpty()) {
            log.trace("No FieldInstanceProvider found for field: '{}' with type: '{}'", field.getName(), field.getType().getName());
            return;
        }
        try {
            fieldInstanceProviderOpt.get().destroyInstance(testInstance, field);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to destroy instance for field '%s' of type: '%s'", field.getName(), field.getType().getName()), e);
        }
    }


    public static <T extends Annotation> String resolveValue(Object testInstance, Field field, Class<T> annClass,
                                                             Function<T, Value> valueExtractor, String... defaultValue) throws IllegalArgumentException {
        return Optional.ofNullable(field.getAnnotation(annClass)).map(ann -> {
            Value v = valueExtractor.apply(ann);
            if (!v.value().isBlank()) {
                return v.value();
            } else if (!v.prop().isBlank()) {
                String resolved = System.getProperty(v.prop());
                if (resolved == null) {
                    if (defaultValue.length > 0 && defaultValue[0] != null) {
                        return defaultValue[0];
                    }
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

    public static <T extends Annotation> int resolveIntValue(Object testInstance, Field field, Class<T> annClass,
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

}
