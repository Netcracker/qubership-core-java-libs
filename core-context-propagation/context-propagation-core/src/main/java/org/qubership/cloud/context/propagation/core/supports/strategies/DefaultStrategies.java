package org.qubership.cloud.context.propagation.core.supports.strategies;

import java.util.function.Supplier;

public class DefaultStrategies {
    public static <V> ThreadLocalWithInheritanceDefaultStrategy<V> threadLocalWithInheritanceDefaultStrategy(){
        return new ThreadLocalWithInheritanceDefaultStrategy<>();
    }

    public static <V> ThreadLocalWithInheritanceDefaultStrategy<V> threadLocalWithInheritanceDefaultStrategy(final Supplier<V> defaultContextObject){
        return new ThreadLocalWithInheritanceDefaultStrategy<>(defaultContextObject);
    }

    public static <V> ThreadLocalDefaultStrategy<V> threadLocalDefaultStrategy(){
        return new ThreadLocalDefaultStrategy<>();
    }

    public static <V> ThreadLocalDefaultStrategy<V> threadLocalDefaultStrategy(final Supplier<V> defaultContextObject){
        return new ThreadLocalDefaultStrategy<>(defaultContextObject);
    }

    public static <V> RestEasyDefaultStrategy<V> restEasyDefaultStrategy(Class<V> tClass, Supplier<V> defaultContextObject){
        return new RestEasyDefaultStrategy<>(tClass, defaultContextObject);
    }

}
