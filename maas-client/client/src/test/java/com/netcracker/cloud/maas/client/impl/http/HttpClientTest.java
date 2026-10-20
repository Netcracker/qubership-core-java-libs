package com.netcracker.cloud.maas.client.impl.http;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpClientTest {

    @Test
    void maasClientRebasesRequestsOntoTheConfiguredAgent() throws Exception {
        String agentUrl = "http://maas-agent-custom:9090";
        withProp(Env.PROP_MAAS_AGENT_URL, agentUrl, () -> {
            M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getMaasClient(() -> "faketoken"));

            assertNotNull(interceptor);
            assertEquals(HttpUrl.get(agentUrl), fallbackBaseUrl(interceptor));
        });
    }

    @Test
    void maasClientFallsBackToTheDefaultAgentAddress() throws Exception {
        withProp(Env.PROP_MAAS_AGENT_URL, null, () -> {
            M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getMaasClient(() -> "faketoken"));

            assertEquals(HttpUrl.get(Env.DEFAULT_MAAS_AGENT_URL), fallbackBaseUrl(interceptor));
        });
    }

    @Test
    void m2mClientKeepsTheRequestedAddress() throws Exception {
        M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getM2mClient(() -> "faketoken"));

        assertNotNull(interceptor);
        assertNull(fallbackBaseUrl(interceptor));
    }

    private M2MInterceptor m2mInterceptorOf(HttpClient httpClient) throws Exception {
        Field field = HttpClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        OkHttpClient okHttpClient = (OkHttpClient) field.get(httpClient);
        return (M2MInterceptor) okHttpClient.interceptors().stream()
                .filter(M2MInterceptor.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    private Object fallbackBaseUrl(M2MInterceptor interceptor) throws Exception {
        Field field = M2MInterceptor.class.getDeclaredField("fallbackBaseUrl");
        field.setAccessible(true);
        return field.get(interceptor);
    }
}
