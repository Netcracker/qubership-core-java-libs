package com.netcracker.cloud.consul.provider.common;

import java.util.concurrent.atomic.AtomicReference;

public class SimpleTokenStorage implements TokenStorage {

    private final AtomicReference<String> token = new AtomicReference<>("");

    public SimpleTokenStorage() {}

    public SimpleTokenStorage(String token) {
        this.token.set(token);
    }

    @Override
    public String get() {
        return token.get();
    }

    @Override
    public void update(String token) {
        this.token.set(token);
    }
}
