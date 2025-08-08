package org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter;

public interface Chain<T> {
    void doFilter(T data);
}
