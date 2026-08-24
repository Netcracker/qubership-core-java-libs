package com.netcracker.cloud.configserver.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.auth.Token;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebClientConfigServerConfigDataLocationResolverTest {

    @Test
    void testM2mClientIsUsedWhenM2MManagerIsRegistered() throws Exception {
        ConfigurableBootstrapContext bootstrapContext = bootstrapContextWithM2M("bootstrap-token");

        MicroserviceRestClient restClient = resolver(bootstrapContext).getMicroserviceRestClient();

        assertInstanceOf(MicroserviceOkHttpRestClient.class, restClient);
        M2MInterceptor interceptor = m2mInterceptorOf((MicroserviceOkHttpRestClient) restClient);
        assertNotNull(interceptor);
        // plain m2m call, the request address is kept as is
        assertNull(fieldValue(interceptor, "fallbackBaseUrl"));
        assertEquals("Bearer bootstrap-token", authHeader(interceptor));
    }

    @Test
    void testWebClientIsUsedWhenM2MManagerIsNotRegistered() {
        ConfigurableBootstrapContext bootstrapContext = mock(ConfigurableBootstrapContext.class);
        when(bootstrapContext.isRegistered(M2MManager.class)).thenReturn(false);

        MicroserviceRestClient restClient = resolver(bootstrapContext).getMicroserviceRestClient();

        assertInstanceOf(MicroserviceWebClient.class, restClient);
    }

    private WebClientConfigServerConfigDataLocationResolver resolver(ConfigurableBootstrapContext bootstrapContext) {
        DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
        return new WebClientConfigServerConfigDataLocationResolver(logFactory, bootstrapContext);
    }

    private ConfigurableBootstrapContext bootstrapContextWithM2M(String tokenValue) {
        Token token = mock(Token.class);
        when(token.getTokenValue()).thenReturn(tokenValue);
        M2MManager m2MManager = mock(M2MManager.class);
        when(m2MManager.getToken()).thenReturn(token);

        ConfigurableBootstrapContext bootstrapContext = mock(ConfigurableBootstrapContext.class);
        when(bootstrapContext.isRegistered(M2MManager.class)).thenReturn(true);
        when(bootstrapContext.get(M2MManager.class)).thenReturn(m2MManager);
        return bootstrapContext;
    }

    private M2MInterceptor m2mInterceptorOf(MicroserviceOkHttpRestClient restClient) throws Exception {
        Field field = MicroserviceOkHttpRestClient.class.getDeclaredField("client");
        field.setAccessible(true);
        OkHttpClient client = (OkHttpClient) field.get(restClient);
        return (M2MInterceptor) client.interceptors().stream()
                .filter(M2MInterceptor.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String authHeader(M2MInterceptor interceptor) throws Exception {
        return ((Supplier<String>) fieldValue(interceptor, "fallbackAuthHeaderSupplier")).get();
    }

    private Object fieldValue(M2MInterceptor interceptor, String name) throws Exception {
        Field field = M2MInterceptor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(interceptor);
    }
}
