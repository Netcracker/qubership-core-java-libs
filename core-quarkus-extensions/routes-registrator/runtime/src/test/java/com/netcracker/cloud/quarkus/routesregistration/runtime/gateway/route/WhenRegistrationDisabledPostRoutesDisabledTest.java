package com.netcracker.cloud.quarkus.routesregistration.runtime.gateway.route;

import com.netcracker.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that postRoutesEnabled is false when the config flag itself is false,
 * even with a non-Istio mesh type.
 */
@QuarkusTest
@TestProfile(TestProfiles.RegistrationExplicitlyDisabledProfile.class)
class WhenRegistrationDisabledPostRoutesDisabledTest {

    @Inject
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Test
    void testPostRoutesEnabled_whenRegistrationDisabled_isFalseRegardlessOfMeshType() {
        assertFalse(routesRestRegistrationProcessor.isPostRoutesEnabled());
    }
}

