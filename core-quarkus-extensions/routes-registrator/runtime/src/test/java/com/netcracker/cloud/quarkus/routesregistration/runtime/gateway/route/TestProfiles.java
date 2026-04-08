package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType;
import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class TestProfiles {
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
     * Overrides SERVICE_MESH_TYPE using pascal-case value.
     * Expected: postRoutesEnabled = true && !isIstioEnabled(istio) = true && false = false
     */
    public static class IstioCaseInsensitiveProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "SERVICE_MESH_TYPE", "Istio",
                    "apigateway.routes.registration.enabled", "true"
            );
        }
    }

}
