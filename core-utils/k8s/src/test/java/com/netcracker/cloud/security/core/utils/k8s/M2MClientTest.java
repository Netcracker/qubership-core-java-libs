package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

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
