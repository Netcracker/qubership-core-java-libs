package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SystemStubsExtension.class)
class M2MClientTest {

    private static final Supplier<String> TOKEN_SUPPLIER = () -> "test-token";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void testK8sM2mEnabledIsReadFromEnvironment() {
        environmentVariables.set("KUBERNETES_M2M_ENABLED", "true");
        assertTrue(M2MClient.isK8sM2mEnabled());
        assertEquals(true, getFieldValue(buildInterceptor(M2MClient.builder()), "k8sM2mEnabled"));

        environmentVariables.set("KUBERNETES_M2M_ENABLED", "false");
        assertFalse(M2MClient.isK8sM2mEnabled());
        assertEquals(false, getFieldValue(buildInterceptor(M2MClient.builder()), "k8sM2mEnabled"));

        environmentVariables.remove("KUBERNETES_M2M_ENABLED");
        assertFalse(M2MClient.isK8sM2mEnabled());

        // explicitly configured flag wins over the environment
        assertEquals(true, getFieldValue(buildInterceptor(M2MClient.builder().k8sM2mEnabled(true)), "k8sM2mEnabled"));
    }

    @Test
    void testClientWithoutAgentUrlDoesNotRebaseRequests() {
        OkHttpClient client = M2MClient.builder()
                .keycloakTokenSupplier(TOKEN_SUPPLIER)
                .build();

        assertNotNull(client);
        M2MInterceptor interceptor = findM2mInterceptor(client);
        assertNotNull(interceptor);

        assertNull(getFieldValue(interceptor, "fallbackBaseUrl"));
    }

    @Test
    void testDbaasClientUsesTheConfiguredAgentUrl() {
        String agentUrl = "http://custom-dbaas-agent:9090";
        OkHttpClient client = M2MClient.builder()
                .audience(AudienceName.DBAAS)
                .agentUrl(agentUrl)
                .keycloakTokenSupplier(TOKEN_SUPPLIER)
                .build();
        M2MInterceptor interceptor = findM2mInterceptor(client);
        assertNotNull(interceptor);

        assertEquals(HttpUrl.get(agentUrl), getFieldValue(interceptor, "fallbackBaseUrl"));
    }

    @Test
    void testMaasClientUsesTheConfiguredAgentUrl() {
        OkHttpClient client = M2MClient.builder()
                .audience(AudienceName.MAAS)
                .agentUrl("http://maas-agent:8080")
                .keycloakTokenSupplier(TOKEN_SUPPLIER)
                .build();
        M2MInterceptor interceptor = findM2mInterceptor(client);
        assertNotNull(interceptor);

        assertEquals(HttpUrl.get("http://maas-agent:8080"), getFieldValue(interceptor, "fallbackBaseUrl"));
    }

    @Test
    void testTokenSupplierIsRequired() {
        M2MClient.M2MClientBuilder builder = M2MClient.builder().audience(AudienceName.DBAAS);
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void testAudienceMustNotBeNull() {
        M2MClient.M2MClientBuilder builder = M2MClient.builder();
        assertThrows(NullPointerException.class, () -> builder.audience(null));
    }

    @Test
    void testAudienceDefaultsToNetcracker() {
        try (var tokenSource = mockStatic(KubernetesAudienceToken.class)) {
            tokenSource.when(() -> KubernetesAudienceToken.getToken(anyString())).thenReturn("k8s-token");

            M2MInterceptor interceptor = findM2mInterceptor(M2MClient.builder()
                    .keycloakTokenSupplier(TOKEN_SUPPLIER)
                    .build());

            assertEquals("Bearer k8s-token", k8sAuthHeader(interceptor));
            tokenSource.verify(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER));
        }
    }

    @Test
    void testAudienceIsPassedToTheKubernetesTokenSource() {
        for (String audience : new String[]{AudienceName.DBAAS, AudienceName.MAAS, AudienceName.NETCRACKER}) {
            try (var tokenSource = mockStatic(KubernetesAudienceToken.class)) {
                tokenSource.when(() -> KubernetesAudienceToken.getToken(anyString())).thenReturn("k8s-token");

                M2MInterceptor interceptor = findM2mInterceptor(M2MClient.builder()
                        .audience(audience)
                        .keycloakTokenSupplier(TOKEN_SUPPLIER)
                        .build());

                assertEquals("Bearer k8s-token", k8sAuthHeader(interceptor));
                tokenSource.verify(() -> KubernetesAudienceToken.getToken(audience));
            }
        }
    }

    @Test
    void testKeycloakTokenIsSentAsBearerHeader() {
        M2MInterceptor interceptor = findM2mInterceptor(M2MClient.builder()
                .keycloakTokenSupplier(TOKEN_SUPPLIER)
                .build());

        assertEquals("Bearer test-token", fallbackAuthHeader(interceptor));
    }

    @Test
    void testTokenSuppliersAreCalledPerRequestAndNotAtBuildTime() {
        AtomicInteger calls = new AtomicInteger();
        M2MInterceptor interceptor = findM2mInterceptor(M2MClient.builder()
                .keycloakTokenSupplier(() -> "token-" + calls.incrementAndGet())
                .build());

        assertEquals(0, calls.get(), "the token must not be requested while the client is being built");
        assertEquals("Bearer token-1", fallbackAuthHeader(interceptor));
        assertEquals("Bearer token-2", fallbackAuthHeader(interceptor));
    }

    @Test
    void testAgentUrlWithoutSchemeIsRejected() {
        M2MClient.M2MClientBuilder builder = M2MClient.builder()
                .agentUrl("localhost:8080")
                .keycloakTokenSupplier(TOKEN_SUPPLIER);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void testEachClientGetsItsOwnUrlCache() {
        M2MClient.M2MClientBuilder builder = M2MClient.builder().keycloakTokenSupplier(TOKEN_SUPPLIER);

        UrlCache first = (UrlCache) getFieldValue(findM2mInterceptor(builder.build()), "urlCache");
        UrlCache second = (UrlCache) getFieldValue(findM2mInterceptor(builder.build()), "urlCache");

        assertNotNull(first);
        assertNotSame(first, second, "clients must not share the fallback decision cache");
    }

    @Test
    void testBuilderMethodsReturnTheSameBuilder() {
        M2MClient.M2MClientBuilder builder = M2MClient.builder();

        assertSame(builder, builder.audience(AudienceName.DBAAS));
        assertSame(builder, builder.agentUrl("http://dbaas-agent:8080"));
        assertSame(builder, builder.keycloakTokenSupplier(TOKEN_SUPPLIER));
        assertSame(builder, builder.k8sM2mEnabled(true));
    }

    @SuppressWarnings("unchecked")
    private String k8sAuthHeader(M2MInterceptor interceptor) {
        return ((Supplier<String>) getFieldValue(interceptor, "k8sAuthHeaderSupplier")).get();
    }

    @SuppressWarnings("unchecked")
    private String fallbackAuthHeader(M2MInterceptor interceptor) {
        return ((Supplier<String>) getFieldValue(interceptor, "fallbackAuthHeaderSupplier")).get();
    }

    private M2MInterceptor buildInterceptor(M2MClient.M2MClientBuilder builder) {
        return findM2mInterceptor(builder.keycloakTokenSupplier(TOKEN_SUPPLIER).build());
    }

    private M2MInterceptor findM2mInterceptor(OkHttpClient client) {
        return (M2MInterceptor) client.interceptors().stream()
                .filter(M2MInterceptor.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    private Object getFieldValue(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
