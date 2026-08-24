package com.netcracker.cloud.dbaas.client.restclient.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.auth.Token;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbaasWebClientConfigurationTest {

    private final DbaasWebClientConfiguration configuration = new DbaasWebClientConfiguration();

    @Test
    void testDbaasRestClientIsRebasedOntoTheConfiguredAgent() throws Exception {
        String agentUrl = "http://dbaas-agent-custom:9090";

        MicroserviceRestClient restClient = configuration.dbaasRestClient(m2mManager("token"), agentUrl);

        assertInstanceOf(MicroserviceOkHttpRestClient.class, restClient);
        M2MInterceptor interceptor = m2mInterceptorOf(restClient);
        assertNotNull(interceptor);
        assertEquals(HttpUrl.get(agentUrl), fallbackBaseUrl(interceptor));
    }

    @Test
    void testDbaasRestClientTakesTheKeycloakTokenFromM2MManager() throws Exception {
        MicroserviceRestClient restClient = configuration.dbaasRestClient(m2mManager("keycloak-token"), "http://dbaas-agent:8080");

        assertEquals("Bearer keycloak-token", fallbackAuthHeader(m2mInterceptorOf(restClient)));
    }

    private M2MManager m2mManager(String tokenValue) {
        Token token = mock(Token.class);
        when(token.getTokenValue()).thenReturn(tokenValue);
        M2MManager m2MManager = mock(M2MManager.class);
        when(m2MManager.getToken()).thenReturn(token);
        return m2MManager;
    }

    private M2MInterceptor m2mInterceptorOf(MicroserviceRestClient restClient) throws Exception {
        Field field = MicroserviceOkHttpRestClient.class.getDeclaredField("client");
        field.setAccessible(true);
        OkHttpClient client = (OkHttpClient) field.get(restClient);
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

    @SuppressWarnings("unchecked")
    private String fallbackAuthHeader(M2MInterceptor interceptor) throws Exception {
        Field field = M2MInterceptor.class.getDeclaredField("fallbackAuthHeaderSupplier");
        field.setAccessible(true);
        return ((java.util.function.Supplier<String>) field.get(interceptor)).get();
    }
}
