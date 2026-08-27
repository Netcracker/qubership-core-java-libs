package com.netcracker.cloud.consul.provider.common;

import java.util.function.Supplier;

final class M2MLoginCredentials implements ConsulLoginCredentials {

    private final String namespace;
    private final Supplier<String> m2mSupplier;

    M2MLoginCredentials(String namespace, Supplier<String> m2mSupplier) {
        this.namespace = namespace;
        this.m2mSupplier = m2mSupplier;
    }

    @Override
    public String getAuthMethod() {
        return namespace;
    }

    @Override
    public String getBearerToken() {
        return m2mSupplier.get();
    }
}
