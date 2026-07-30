package com.netcracker.cloud.security.core.utils.k8s.localdev;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class LocalDevModeTest {

    @SystemStub
    private SystemProperties systemProperties;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @AfterEach
    void clear() {
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
        System.clearProperty(LocalDevMode.SPRING_PROFILES_ACTIVE_PROPERTY);
        System.clearProperty(LocalDevMode.MICROSERVICE_NAME_PROPERTY);
    }

    @Test
    void disabledByDefault() {
        assertFalse(LocalDevMode.isEnabled());
    }

    @Test
    void enabledByQuarkusProfileProperty() {
        systemProperties.set(LocalDevMode.QUARKUS_PROFILE_PROPERTY, "dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void enabledByQuarkusProfileEnv() {
        environmentVariables.set(LocalDevMode.QUARKUS_PROFILE_ENV, "dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void enabledBySpringProfilesActiveProperty() {
        systemProperties.set(LocalDevMode.SPRING_PROFILES_ACTIVE_PROPERTY, "local,dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void enabledBySpringProfilesActiveEnv() {
        environmentVariables.set(LocalDevMode.SPRING_PROFILES_ACTIVE_ENV, "local,dev");
        assertTrue(LocalDevMode.isEnabled());
    }

    @Test
    void requireMicroserviceNameFromProperty() {
        systemProperties.set(LocalDevMode.MICROSERVICE_NAME_PROPERTY, "my-service");
        assertEquals("my-service", LocalDevMode.requireMicroserviceName());
    }

    @Test
    void requireMicroserviceNameFailsWhenMissing() {
        assertThrows(IllegalStateException.class, LocalDevMode::requireMicroserviceName);
    }

    @Test
    void requireNamespaceFromEnv() {
        environmentVariables.set(LocalDevMode.NAMESPACE_ENV, "my-ns");
        assertEquals("my-ns", LocalDevMode.requireNamespace());
    }

    @Test
    void requireMicroserviceNameFromEnv() {
        environmentVariables.set(LocalDevMode.MICROSERVICE_NAME_ENV, "env-service");
        assertEquals("env-service", LocalDevMode.requireMicroserviceName());
    }

    @Test
    void requireNamespaceFailsWhenMissing() {
        assertThrows(IllegalStateException.class, LocalDevMode::requireNamespace);
    }
}
