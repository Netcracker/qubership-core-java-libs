package com.netcracker.cloud.consul.provider.common;


public interface TokenStorage {
    String get();

    void update(String token);
}
