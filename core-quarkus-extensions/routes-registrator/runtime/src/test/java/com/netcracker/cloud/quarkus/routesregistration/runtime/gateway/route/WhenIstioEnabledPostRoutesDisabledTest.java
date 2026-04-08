package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that postRoutesEnabled is forced to false when SERVICE_MESH_TYPE=ISTIO,
 * regardless of the apigateway.routes.registration.enabled flag.
 */
@QuarkusTest
@TestProfile(TestProfiles.IstioEnabledProfile.class)
class WhenIstioEnabledPostRoutesDisabledTest {

    @Inject
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    void testPostRoutesEnabled_whenMeshTypeIsIstio_isFalseRegardlessOfConfigFlag() {
        assertFalse(routesRestRegistrationProcessor.isPostRoutesEnabled());
    }
}
