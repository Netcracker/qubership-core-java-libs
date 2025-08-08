package org.qubership.cloud.maas.client.impl.http;

@FunctionalInterface
public interface OmnivoreFunction<T, R> {
    R apply(T value) throws Exception;
}
