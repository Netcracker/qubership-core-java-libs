package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class M2MClientFactoryTest {

    @Test
    void testConstants() {
        assertEquals("com.netcracker.cloud.dbaas.agent.url", M2MClientFactory.DBAAS_AGENT_URL_PROP);
        assertEquals("com.netcracker.cloud.maas.agent.url", M2MClientFactory.MAAS_AGENT_URL_PROP);
    }

    @Test
    void testGetM2mOkHttpClient() {
        Supplier<String> tokenSupplier = () -> "test-token";
        OkHttpClient client = M2MClientFactory.getM2mOkHttpClient(tokenSupplier);

        assertNotNull(client);
        M2MInterceptor interceptor = findM2mInterceptor(client);
        assertNotNull(interceptor);
        
        assertNull(getFieldValue(interceptor, "fallbackBaseUrl"));
    }

    @Test
    void testGetDbaasOkHttpClientDefault() throws Exception {
        SystemPropertiesTestHelper.withProperty(Map.of(), () -> {
            // Ensure property is cleared
            System.clearProperty(M2MClientFactory.DBAAS_AGENT_URL_PROP);
            
            OkHttpClient client = M2MClientFactory.getDbaasOkHttpClient(() -> "token");
            M2MInterceptor interceptor = findM2mInterceptor(client);
            assertNotNull(interceptor);
            
            assertEquals(HttpUrl.get("http://dbaas-agent:8080"), getFieldValue(interceptor, "fallbackBaseUrl"));
        });
    }

    @Test
    void testGetDbaasOkHttpClientWithProperty() throws Exception {
        String agentUrl = "http://custom-dbaas-agent:9090";
        SystemPropertiesTestHelper.withProperty(Map.of(M2MClientFactory.DBAAS_AGENT_URL_PROP, agentUrl), () -> {
            OkHttpClient client = M2MClientFactory.getDbaasOkHttpClient(() -> "token");
            M2MInterceptor interceptor = findM2mInterceptor(client);
            assertNotNull(interceptor);
            
            assertEquals(HttpUrl.get(agentUrl), getFieldValue(interceptor, "fallbackBaseUrl"));
        });
    }

    @Test
    void testGetMaasOkHttpClientDefault() throws Exception {
        SystemPropertiesTestHelper.withProperty(Map.of(), () -> {
            System.clearProperty(M2MClientFactory.MAAS_AGENT_URL_PROP);
            
            OkHttpClient client = M2MClientFactory.getMaasOkHttpClient(() -> "token");
            M2MInterceptor interceptor = findM2mInterceptor(client);
            assertNotNull(interceptor);
            
            assertEquals(HttpUrl.get("http://maas-agent:8080"), getFieldValue(interceptor, "fallbackBaseUrl"));
        });
    }

    @Test
    void testGetMaasOkHttpClientWithProperty() throws Exception {
        String agentUrl = "http://custom-maas-agent:7070";
        SystemPropertiesTestHelper.withProperty(Map.of(M2MClientFactory.MAAS_AGENT_URL_PROP, agentUrl), () -> {
            OkHttpClient client = M2MClientFactory.getMaasOkHttpClient(() -> "token");
            M2MInterceptor interceptor = findM2mInterceptor(client);
            assertNotNull(interceptor);
            
            assertEquals(HttpUrl.get(agentUrl), getFieldValue(interceptor, "fallbackBaseUrl"));
        });
    }

    private M2MInterceptor findM2mInterceptor(OkHttpClient client) {
        return (M2MInterceptor) client.interceptors().stream()
                .filter(i -> i instanceof M2MInterceptor)
                .findFirst()
                .orElse(null);
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
