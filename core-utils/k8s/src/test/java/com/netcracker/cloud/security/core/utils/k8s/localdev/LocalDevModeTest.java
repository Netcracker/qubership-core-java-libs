package com.netcracker.cloud.security.core.utils.k8s.localdev;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class LocalDevModeTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

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
    void enabledBySpringProfilesActiveProperty() {
        System.setProperty(LocalDevMode.SPRING_PROFILES_ACTIVE_PROPERTY, "local,dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void profileEnvVarsDoNotEnableWithoutSystemProperty() {
        assertFalse(LocalDevMode.isEnabled());
    }
}
