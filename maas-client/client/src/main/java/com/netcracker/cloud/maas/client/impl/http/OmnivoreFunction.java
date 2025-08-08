package com.netcracker.cloud.maas.client.impl.http;

@FunctionalInterface
public interface OmnivoreFunction<T, R> {
    R apply(T value) throws Exception;
}
