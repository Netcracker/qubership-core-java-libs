package com.netcracker.cloud.consul.provider.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M2MLoginCredentialsTest {

    @Test
    void authMethodIsNamespace() {
        M2MLoginCredentials credentials = new M2MLoginCredentials("test-namespace", () -> "test-m2m-token");
        assertEquals("test-namespace", credentials.authMethod());
    }

    @Test
    void bearerTokenAsksSupplierOnEveryCall() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> supplier = () -> "test-m2m-token-" + calls.incrementAndGet();

        M2MLoginCredentials credentials = new M2MLoginCredentials("test-namespace", supplier);

        assertEquals("test-m2m-token-1", credentials.bearerToken());
        assertEquals("test-m2m-token-2", credentials.bearerToken());
        assertEquals(2, calls.get());
    }

    @Test
    void bearerTokenSupplierIsNotAskedOnCreation() {
        AtomicInteger calls = new AtomicInteger();

        new M2MLoginCredentials("test-namespace", () -> {
            calls.incrementAndGet();
            return "test-m2m-token";
        });

        assertEquals(0, calls.get());
    }
}
