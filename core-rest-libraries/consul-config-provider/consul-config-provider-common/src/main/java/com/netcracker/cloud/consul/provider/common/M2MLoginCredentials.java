package com.netcracker.cloud.consul.provider.common;

import java.util.function.Supplier;

/**
 * Credentials of the m2m way, kept for the migration period. The auth method is the namespace of the microservice,
 * and the supplier resolves the M2M token lazily, so the ConfigData phase does not touch the bootstrap registry
 * until a login actually happens.
 */
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
