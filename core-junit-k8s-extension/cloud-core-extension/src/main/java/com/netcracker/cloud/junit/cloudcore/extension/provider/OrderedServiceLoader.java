package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public class OrderedServiceLoader {

    public enum SortByPriority {
        ASC, DESC
    }

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

    public static <T> List<T> loadAll(Class<T> clazz, SortByPriority sort) {
        return ServiceLoader.load(clazz)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.<T, Integer>comparing(instance ->
                        Optional.ofNullable(instance.getClass().getDeclaredAnnotation(Priority.class))
                                .map(Priority::value)
                                .orElse(Integer.MAX_VALUE),
                        sort == SortByPriority.ASC ? Comparator.reverseOrder() : Comparator.naturalOrder()))
                .toList();
    }
}
