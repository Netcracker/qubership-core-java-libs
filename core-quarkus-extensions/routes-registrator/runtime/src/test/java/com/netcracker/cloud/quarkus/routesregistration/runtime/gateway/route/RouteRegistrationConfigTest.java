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

    @Inject
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    public void testCreateAdditionalOkHttpClientBean_isNotConflictedWithNamedBean() {
        Assertions.assertNotSame(controlPlaneHttpClient, anotherControlPlaneHttpClient);
    }

    @Test
    public void testControlPlaneClientInjects() {
        Assertions.assertNotNull(controlPlaneClient);
    }

    /**
     * Default profile has SERVICE_MESH_TYPE=CORE and apigateway.routes.registration.enabled=true,
     * so postRoutesEnabled = true && !isIstioEnabled(CORE) = true && true = true
     */
    @Test
    public void testPostRoutesEnabled_whenMeshTypeIsCore_andRegistrationEnabled() {
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

    /**
     * Tests that postRoutesEnabled is forced to false when SERVICE_MESH_TYPE=ISTIO,
     * regardless of the apigateway.routes.registration.enabled flag.
     */
    @QuarkusTest
    @TestProfile(RouteRegistrationConfigTest.IstioEnabledProfile.class)
    public static class WhenIstioEnabledPostRoutesDisabledTest {

        @Inject
        RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

        @Test
        public void testPostRoutesEnabled_whenMeshTypeIsIstio_isFalseRegardlessOfConfigFlag() {
            Assertions.assertFalse(routesRestRegistrationProcessor.isPostRoutesEnabled());
        }
    }

    /**
     * Tests that postRoutesEnabled is false when the config flag itself is false,
     * even with a non-Istio mesh type.
     */
    @QuarkusTest
    @TestProfile(RouteRegistrationConfigTest.RegistrationExplicitlyDisabledProfile.class)
    public static class WhenRegistrationDisabledPostRoutesDisabledTest {

        @Inject
        RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

        @Test
        public void testPostRoutesEnabled_whenRegistrationDisabled_isFalseRegardlessOfMeshType() {
            Assertions.assertFalse(routesRestRegistrationProcessor.isPostRoutesEnabled());
        }
    }

    /**
     * Overrides SERVICE_MESH_TYPE to ISTIO.
     * Expected: postRoutesEnabled = true && !isIstioEnabled(ISTIO) = true && false = false
     */
    public static class IstioEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "SERVICE_MESH_TYPE", ServiceMeshType.ISTIO.name(),
                    "apigateway.routes.registration.enabled", "true"
            );
        }
    }

    /**
     * Keeps SERVICE_MESH_TYPE=CORE but disables registration via config.
     * Expected: postRoutesEnabled = false && !isIstioEnabled(CORE) = false && true = false
     */
    public static class RegistrationExplicitlyDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "SERVICE_MESH_TYPE", ServiceMeshType.CORE.name(),
                    "apigateway.routes.registration.enabled", "false"
            );
        }
    }
}
