package com.netcracker.cloud.security.core.utils.k8s.localdev;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDevModeTest {

    @AfterEach
    void clear() {
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
        System.clearProperty(LocalDevMode.SPRING_PROFILES_ACTIVE_PROPERTY);
    }

    @Test
    void disabledByDefault() {
        assertFalse(LocalDevMode.isEnabled());
    }

    @Test
    void enabledByQuarkusProfileProperty() {
        System.setProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY, "dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void springDevelopmentProfileDoesNotEnable() {
        System.setProperty(LocalDevMode.SPRING_PROFILES_ACTIVE_PROPERTY, "development");
        assertFalse(LocalDevMode.isEnabled());
    }
}
