package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import com.netcracker.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route.RouteRegistrationConfig.CONTROL_PLANE_HTTP_CLIENT;


@QuarkusTest
class RouteRegistrationConfigTest {

    private static final String ANOTHER_CONTROL_PLANE_HTTP_CLIENT = "anotherControlPlaneHttpClient";

    @Inject
    RouteRegistrationConfig config;

    @Inject
    @Named(ANOTHER_CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient anotherControlPlaneHttpClient;

    @Inject
    @Named(CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient controlPlaneHttpClient;

    @Inject
    ControlPlaneClient controlPlaneClient;

    @Inject
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    void testCreateAdditionalOkHttpClientBean_isNotConflictedWithNamedBean() {
        Assertions.assertNotSame(controlPlaneHttpClient, anotherControlPlaneHttpClient);
    }

    @Test
    void testControlPlaneClientInjects() {
        Assertions.assertNotNull(controlPlaneClient);
    }

    /**
     * Default profile has SERVICE_MESH_TYPE=CORE and apigateway.routes.registration.enabled=true,
     * so postRoutesEnabled = true && !isIstioEnabled(CORE) = true && true = true
     */
    @Test
    void testPostRoutesEnabled_whenMeshTypeIsCore_andRegistrationEnabled() {
        Assertions.assertTrue(routesRestRegistrationProcessor.isPostRoutesEnabled());
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
