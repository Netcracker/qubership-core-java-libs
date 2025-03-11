package org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route.RouteRegistrationConfig.CONTROL_PLANE_HTTP_CLIENT;


@QuarkusTest
public class RouteRegistrationConfigTest {

    private static final String ANOTHER_CONTROL_PLANE_HTTP_CLIENT = "anotherControlPlaneHttpClient";

    @Inject
    @Named(ANOTHER_CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient anotherControlPlaneHttpClient;

    @Inject
    @Named(CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient controlPlaneHttpClient;

    @Inject
    ControlPlaneClient controlPlaneClient;

    @Test
    public void testCreateAdditionalOkHttpClientBean_isNotConflictedWithNamedBean() {
        Assertions.assertNotSame(controlPlaneHttpClient, anotherControlPlaneHttpClient);
    }

    @Test
    public void testControlPlaneClientInjects() {
        Assertions.assertNotNull(controlPlaneClient);
    }

     // idk why, but in QuarkusTest we cannot declare bean via producer method and inject it in that class
     // (it says that circular dependencies created)
    @ApplicationScoped
    private static final class ControlPlaneHttpClientTestConfig {
        @Produces
        @Named(ANOTHER_CONTROL_PLANE_HTTP_CLIENT)
        OkHttpClient testControlPlaneHttpClient() {
            return new OkHttpClient.Builder().build();
        }
    }
}
