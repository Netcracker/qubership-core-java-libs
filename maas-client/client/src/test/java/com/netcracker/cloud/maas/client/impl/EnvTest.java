package com.netcracker.cloud.maas.client.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.qubership.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

class EnvTest {
    @Test
    void testApiUrl() {
        withProp(Env.PROP_API_URL, null, () ->
                assertEquals("http://maas-agent:8080", Env.apiUrl())
        );
    }

    @Test
    void testApiUrlOverride() {
        withProp(Env.PROP_API_URL, "http://localhost:8080/", () ->
                assertEquals("http://localhost:8080", Env.apiUrl())
        );
    }

    @Test
    void testApiUrlWrongOverride() {
        withProp(Env.PROP_API_URL, "localhost:8080", () ->
                assertThrows(IllegalArgumentException.class, () -> Env.apiUrl())
        );
    }

    @Test
    void testTenantManagerReconnectTimeoutDefaults() {
        withProp(Env.PROP_TENANT_MANAGER_RECONNECT_TIMEOUT, null, () ->
                assertEquals(15000L, Env.tenantManagerReconnectTimeout())
        );
    }

    @Test
    void testTenantManagerReconnectTimeoutOverride() {
        withProp(Env.PROP_TENANT_MANAGER_RECONNECT_TIMEOUT, "PT1M", () ->
                assertEquals(60000L, Env.tenantManagerReconnectTimeout())
        );
    }

    @Test
    void testUrl2ws() {
        assertEquals("ws://localhost:8080", Env.url2ws("http://localhost:8080"));
    }

    @Test
    void testUrl2ws_https() {
        assertEquals("wss://localhost:8080", Env.url2ws("https://localhost:8080"));
    }

    @Test
    void testNamespace() {
        withProp(Env.PROP_NAMESPACE, null, () -> {
            var value = withEnvironmentVariable(Env.ENV_CLOUD_NAMESPACE, "abc")
                    .execute(() -> Env.namespace());

            assertEquals("abc", value);
        });
    }

    @Test
    void testNamespaceNewProp() {
        withProp(Env.PROP_CLOUD_NAMESPACE, "prop-namespace-test", () -> {
            var value = withEnvironmentVariable(Env.ENV_CLOUD_NAMESPACE, "abc")
                    .execute(() -> Env.namespace());

            assertEquals("prop-namespace-test", value);
        });
    }

    @Test
    void testOriginNamespaceProp() {
        withProp(Env.PROP_ORIGIN_NAMESPACE, "prop-origin-test", () -> {
            var value = withEnvironmentVariable(Env.ENV_ORIGIN_NAMESPACE, "env-origin-test")
                    .execute(Env::originNamespace);

            assertEquals("prop-origin-test", value);
        });
    }

    @Test
    void testOriginNamespaceNewProp() {
        withProp("origin.namespace", "prop-origin-test", () -> {
            var value = withEnvironmentVariable("origin.namespace", "env-origin-test")
                    .execute(Env::originNamespace);

            assertEquals("prop-origin-test", value);
        });
    }

    @Test
    void testOriginNamespaceEnv() {
        withProp(Env.PROP_ORIGIN_NAMESPACE, null, () -> {
            var value = withEnvironmentVariable(Env.ENV_ORIGIN_NAMESPACE, "env-origin-test")
                    .execute(Env::originNamespace);

            assertEquals("env-origin-test", value);
        });
    }

    @Test
    void testNoOriginNamespace() {
        Assertions.assertThrows(IllegalStateException.class, Env::originNamespace);
    }

    @Test
    void testMicroserviceName() throws Exception {
        var value = withEnvironmentVariable(Env.ENV_MICROSERVICE_NAME, "abc")
                .execute(Env::microserviceName);
        assertEquals("abc", value);
    }
}
