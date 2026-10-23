package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@ExtendWith(SystemStubsExtension.class)
class ConsulLoginModeTest {

    private static final String UNSUPPORTED_M2M_AUTH_MODE =
            "M2M_AUTH_MODE has unsupported value \"cloud-foundry\": set it to legacy, hybrid, or k8s";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void absentModeIsReadFromTheM2MAuthModeVariable() {
        environmentVariables.set(M2MAuthMode.M2M_AUTH_MODE_ENV, "hybrid");

        Assertions.assertEquals(ConsulLoginMode.HYBRID, ConsulLoginMode.resolve(null));
    }

    @Test
    void absentModeIsLegacyWhenTheM2MAuthModeVariableIsUnset() {
        environmentVariables.remove(M2MAuthMode.M2M_AUTH_MODE_ENV);

        Assertions.assertEquals(ConsulLoginMode.LEGACY, ConsulLoginMode.resolve(null));
    }

    private static Supplier<M2MAuthMode> unsupportedM2MAuthMode() {
        return () -> {
            throw new IllegalArgumentException(UNSUPPORTED_M2M_AUTH_MODE);
        };
    }

    @Test
    void absentModeIsTakenFromM2MAuthMode() {
        Map<M2MAuthMode, ConsulLoginMode> expected = Map.of(
                M2MAuthMode.LEGACY, ConsulLoginMode.LEGACY,
                M2MAuthMode.HYBRID, ConsulLoginMode.HYBRID,
                M2MAuthMode.K8S, ConsulLoginMode.K8S);

        for (M2MAuthMode m2m : M2MAuthMode.values()) {
            Assertions.assertEquals(expected.get(m2m), ConsulLoginMode.resolve(null, () -> m2m), "M2M_AUTH_MODE " + m2m);
        }
    }

    @Test
    void configuredModeOverridesEveryM2MAuthMode() {
        for (ConsulLoginMode configured : ConsulLoginMode.values()) {
            for (M2MAuthMode m2m : M2MAuthMode.values()) {
                Assertions.assertEquals(configured, ConsulLoginMode.resolve(configured, () -> m2m),
                        "consul.auth.mode " + configured + ", M2M_AUTH_MODE " + m2m);
            }
        }
    }

    @Test
    void m2mAuthModeIsNotReadWhenTheModeIsConfigured() {
        AtomicInteger reads = new AtomicInteger();

        ConsulLoginMode resolved = ConsulLoginMode.resolve(ConsulLoginMode.HYBRID, () -> {
            reads.incrementAndGet();
            return M2MAuthMode.K8S;
        });

        Assertions.assertEquals(ConsulLoginMode.HYBRID, resolved);
        Assertions.assertEquals(0, reads.get(), "reads of M2M_AUTH_MODE");
    }

    @Test
    void unsupportedM2MAuthModeNamesBothSettings() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ConsulLoginMode.resolve(null, unsupportedM2MAuthMode()));

        Assertions.assertTrue(thrown.getMessage().contains("consul.auth.mode is not set"), thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains(UNSUPPORTED_M2M_AUTH_MODE), thrown.getMessage());
    }

    @Test
    void unsupportedM2MAuthModeIsIgnoredWhenTheModeIsConfigured() {
        Assertions.assertEquals(ConsulLoginMode.LEGACY,
                ConsulLoginMode.resolve(ConsulLoginMode.LEGACY, unsupportedM2MAuthMode()));
    }
}
