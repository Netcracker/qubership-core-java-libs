package com.netcracker.cloud.junit.cloudcore.extension.provider;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Conditional;
import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;

import java.util.*;

public class OrderedServiceLoader {

    public enum SortByPriority {
        ASC, DESC
    }

    public static <T> Optional<T> load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .stream()
                .map(ServiceLoader.Provider::get)
                .min(Comparator.comparing((T instance) -> !isConditional(instance)
                ).thenComparingInt(OrderedServiceLoader::getPriority));
    }


    private static boolean isConditional(Object instance) {
        Conditional annotation = instance.getClass().getDeclaredAnnotation(Conditional.class);
        if (annotation == null) {
            return false;
        }
        return "true".equalsIgnoreCase(System.getenv(annotation.value()));
    }

    private static int getPriority(Object instance) {
        Priority priority = instance.getClass().getDeclaredAnnotation(Priority.class);
        return priority == null ? Integer.MAX_VALUE : priority.value();
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
