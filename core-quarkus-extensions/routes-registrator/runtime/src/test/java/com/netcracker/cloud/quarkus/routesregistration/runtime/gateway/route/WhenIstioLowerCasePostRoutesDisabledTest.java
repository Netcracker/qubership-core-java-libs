package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that SERVICE_MESH_TYPE is parsed case-insensitively.
 * Example: "Istio" should be treated the same as "ISTIO".
 */
@QuarkusTest
@TestProfile(TestProfiles.IstioCaseInsensitiveProfile.class)
class WhenIstioLowerCasePostRoutesDisabledTest {

    @Inject
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    void testPostRoutesEnabled_whenMeshTypeIsIstioLowerCase_isFalse() {
        assertFalse(routesRestRegistrationProcessor.isPostRoutesEnabled());
    }
}
