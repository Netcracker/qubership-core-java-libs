package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;

import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;

public class OrderedServiceLoader {

    public static <T> Optional<T> load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .stream()
                .map(ServiceLoader.Provider::get)
                .min(Comparator.comparing(instance -> {
                    Priority priority = instance.getClass().getDeclaredAnnotation(Priority.class);
                    if (priority == null) {
                        return Integer.MAX_VALUE;
                    } else {
                        return priority.value();
                    }
                }));
    }
}
