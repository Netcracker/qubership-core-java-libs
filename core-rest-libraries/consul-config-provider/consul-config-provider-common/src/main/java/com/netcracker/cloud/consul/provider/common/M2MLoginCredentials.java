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
    public String authMethod() {
        return namespace;
    }

    @Override
    public String bearerToken() {
        return m2mSupplier.get();
    }
}
