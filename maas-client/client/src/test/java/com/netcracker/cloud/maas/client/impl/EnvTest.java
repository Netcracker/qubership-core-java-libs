package com.netcracker.cloud.maas.client.impl;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

class EnvTest {
    @Test
    void testApiUrl() {
        withProp(Env.PROP_MAAS_AGENT_URL, null, () ->
                assertEquals("http://maas-agent:8080", Env.apiUrl())
        );
    }

    @Test
    void testApiUrlOverride() {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://localhost:8080/", () ->
                assertEquals("http://localhost:8080", Env.apiUrl())
        );
    }

    @Test
    void testApiUrlWrongOverride() {
        withProp(Env.PROP_MAAS_AGENT_URL, "localhost:8080", () ->
                assertThrows(IllegalArgumentException.class, Env::apiUrl)
        );
    }

    @Test
    void testMaasAgentUrlDefault() {
        withProp(Env.PROP_MAAS_AGENT_URL, null, () ->
                assertEquals(Env.DEFAULT_MAAS_AGENT_URL, Env.maasAgentUrl())
        );
    }

    @Test
    void testMaasAgentUrlOverrideIsNormalized() {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://localhost:8080/", () ->
                assertEquals("http://localhost:8080", Env.maasAgentUrl())
        );
    }

    @Test
    void testMaasAgentUrlWrongOverride() {
        withProp(Env.PROP_MAAS_AGENT_URL, "localhost:8080", () ->
                assertThrows(IllegalArgumentException.class, Env::maasAgentUrl)
        );
    }

    @Test
    void testApiUrlFallsBackToTheAgentWhenMaasUrlIsNotSet() {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://maas-agent-custom:8080", () ->
                withProp(Env.PROP_MAAS_URL, null, () -> {
                    assertEquals("http://maas-agent-custom:8080", Env.apiUrl(M2MAuthMode.HYBRID));
                    assertEquals("http://maas-agent-custom:8080", Env.apiUrl(M2MAuthMode.LEGACY));
                })
        );
    }

    @Test
    void testApiUrlFallsBackToTheAgentWhenMaasUrlIsEmpty() {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://maas-agent-custom:8080", () ->
                withProp(Env.PROP_MAAS_URL, "", () -> {
                    assertEquals("http://maas-agent-custom:8080", Env.apiUrl(M2MAuthMode.HYBRID));
                    assertEquals("http://maas-agent-custom:8080", Env.apiUrl(M2MAuthMode.LEGACY));
                })
        );
    }

    @Test
    void testApiUrlHybridModeUsesTheMaasUrl() {
        withProp(Env.PROP_MAAS_URL, "http://localhost:8080/", () ->
                assertEquals("http://localhost:8080", Env.apiUrl(M2MAuthMode.HYBRID))
        );
    }

    @Test
    void testApiUrlLegacyModeIgnoresTheMaasUrl() {
        withProp(Env.PROP_MAAS_AGENT_URL, null, () ->
                withProp(Env.PROP_MAAS_URL, "localhost:8080", () ->
                        assertEquals("http://maas-agent:8080", Env.apiUrl(M2MAuthMode.LEGACY))
                )
        );
    }

    @Test
    void testApiUrlK8sModeUsesTheMaasUrl() {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://maas-agent-custom:8080", () ->
                withProp(Env.PROP_MAAS_URL, "http://localhost:8080/", () ->
                        assertEquals("http://localhost:8080", Env.apiUrl(M2MAuthMode.K8S))
                )
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testApiUrlK8sModeRejectsMissingMaasUrl(String maasUrl) {
        withProp(Env.PROP_MAAS_AGENT_URL, "http://maas-agent-custom:8080", () ->
                withProp(Env.PROP_MAAS_URL, maasUrl, () -> {
                    IllegalStateException e = assertThrows(IllegalStateException.class, () -> Env.apiUrl(M2MAuthMode.K8S));
                    assertTrue(e.getMessage().startsWith(Env.PROP_MAAS_URL + " is not set"), e.getMessage());
                })
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
                    .execute(Env::namespace);

            assertEquals("abc", value);
        });
    }

    @Test
    void testNamespaceNewProp() {
        withProp(Env.PROP_CLOUD_NAMESPACE, "prop-namespace-test", () -> {
            var value = withEnvironmentVariable(Env.ENV_CLOUD_NAMESPACE, "abc")
                    .execute(Env::namespace);

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

    @Test
    void testHttpRetryMaxTotalDurationDefault() {
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, null, () ->
                assertEquals(Env.DEFAULT_HTTP_RETRY_MAX_TOTAL_DURATION_MS,
                        Env.httpRetryMaxTotalDuration().toMillis()));
    }

    @Test
    void testHttpRetryMaxTotalDurationIsRead() {
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, " 1500 ", () ->
                assertEquals(1500, Env.httpRetryMaxTotalDuration().toMillis(), "surrounding spaces are tolerated"));
        withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, "0", () ->
                assertEquals(0, Env.httpRetryMaxTotalDuration().toMillis(), "zero disables retries"));
    }

    /** An unusable value must not fail the call that happens to be first; it falls back and warns. */
    @Test
    void testHttpRetryMaxTotalDurationFallsBackOnUnusableValue() {
        for (String raw : new String[]{"soon", "", "-1"}) {
            withProp(Env.PROP_HTTP_RETRY_MAX_TOTAL_DURATION_MS, raw, () ->
                    assertEquals(Env.DEFAULT_HTTP_RETRY_MAX_TOTAL_DURATION_MS,
                            Env.httpRetryMaxTotalDuration().toMillis(), "unusable value: '" + raw + "'"));
        }
    }
}
