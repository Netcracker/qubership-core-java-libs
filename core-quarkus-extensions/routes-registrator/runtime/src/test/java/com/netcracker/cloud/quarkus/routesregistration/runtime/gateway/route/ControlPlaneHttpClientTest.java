package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPlaneHttpClientTest {

    private final RouteRegistrationConfig config = new RouteRegistrationConfig(
            "test-service", "test-namespace", Optional.empty(), false, "8080", true, Optional.empty(), Optional.empty());

    @Test
    void testControlPlaneHttpClientTalksPlainM2m() throws Exception {
        OkHttpClient client = config.controlPlaneHttpClient();

        M2MInterceptor interceptor = (M2MInterceptor) client.interceptors().stream()
                .filter(M2MInterceptor.class::isInstance)
                .findFirst()
                .orElse(null);

        assertNotNull(interceptor, "control-plane client must carry the m2m interceptor");
        // control-plane is a plain microservice call, requests keep the address they were made with
        assertNull(fallbackBaseUrl(interceptor));
        assertTrue(client.retryOnConnectionFailure());
    }

    private Object fallbackBaseUrl(M2MInterceptor interceptor) throws Exception {
        Field field = M2MInterceptor.class.getDeclaredField("fallbackBaseUrl");
        field.setAccessible(true);
        return field.get(interceptor);
    }
}
