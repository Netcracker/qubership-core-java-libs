package com.netcracker.cloud.junit.cloudcore.extension.service;

public interface BasePortForwardParams<T> {

    String getName();

    int getPort();

    String getNamespace();

    String host(String namespace);

    T supply(NetSocketAddress netSocketAddress);
}
