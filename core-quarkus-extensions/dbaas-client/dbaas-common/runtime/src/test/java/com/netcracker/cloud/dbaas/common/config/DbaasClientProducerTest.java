package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbaasClientProducerTest {

    @Test
    void testDefaultDbaasAgentAddress() {
        assertEquals("http://dbaas-agent:8080", DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS);
    }

    @Test
    void testProducedClientIsRebasedOntoTheConfiguredAgent() throws Exception {
        String agentUrl = "http://dbaas-agent-custom:9090";

        OkHttpClient client = new DbaasClientProducer().dbaasOkHttpClient(dbaasClientConfig(agentUrl));

        M2MInterceptor interceptor = m2mInterceptorOf(client);
        assertNotNull(interceptor, "the produced client must carry the m2m interceptor");
        assertEquals(HttpUrl.get(agentUrl), fallbackBaseUrl(interceptor));
    }

    @Test
    void testProducedClientUsesTheDefaultAgentAddress() throws Exception {
        OkHttpClient client = new DbaasClientProducer()
                .dbaasOkHttpClient(dbaasClientConfig(DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS));

        assertEquals(HttpUrl.get(DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS), fallbackBaseUrl(m2mInterceptorOf(client)));
    }

    private DbaasClientConfig dbaasClientConfig(String dbaasAgentUrl) {
        DbaasClientConfig dbaasClientConfig = mock(DbaasClientConfig.class);
        when(dbaasClientConfig.dbaasAgentUrl()).thenReturn(dbaasAgentUrl);
        return dbaasClientConfig;
    }

    private M2MInterceptor m2mInterceptorOf(OkHttpClient client) {
        return (M2MInterceptor) client.interceptors().stream()
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
