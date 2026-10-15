package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

class ConsulLoginModeConfigTest {

    private static SmallRyeConfig configWithMode(String mode) {
        return new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of(ConsulClientConfiguration.PROP_AUTH_MODE, mode))
                .build();
    }

    private static ConsulLoginMode read(String mode) {
        return configWithMode(mode).getValue(ConsulClientConfiguration.PROP_AUTH_MODE, ConsulLoginMode.class);
    }

    @Test
    void everyModeIsReadFromItsPropertyValue() {
        Assertions.assertEquals(ConsulLoginMode.K8S, read("k8s"));
        Assertions.assertEquals(ConsulLoginMode.LEGACY, read("legacy"));
        Assertions.assertEquals(ConsulLoginMode.HYBRID, read("hybrid"));
    }

    @Test
    void unknownModeBreaksTheStart() {
        SmallRyeConfig config = configWithMode("cloud-foundry");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> config.getValue(ConsulClientConfiguration.PROP_AUTH_MODE, ConsulLoginMode.class));
    }

    @Test
    void oldModeValuesBreakTheStart() {
        for (String mode : new String[]{"kubernetes-with-m2m-fallback", "kubernetes", "m2m"}) {
            SmallRyeConfig config = configWithMode(mode);

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> config.getValue(ConsulClientConfiguration.PROP_AUTH_MODE, ConsulLoginMode.class), mode);
        }
    }

    @Test
    void anEmptyModeIsReadAsAbsent() {
        Assertions.assertEquals(Optional.empty(), configWithMode("")
                .getOptionalValue(ConsulClientConfiguration.PROP_AUTH_MODE, ConsulLoginMode.class));
    }

    /**
     * The plain SmallRye config of this test converts an ISO-8601 duration only. The simple form the documentation
     * gives, {@code 5h}, needs the converter Quarkus registers, and
     * {@link ConsulClientConfigurationKubernetesModeTest} reads it in a running application.
     */
    @Test
    void theFallbackRecheckIntervalIsReadFromItsPropertyValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of(ConsulClientConfiguration.PROP_AUTH_FALLBACK_RECHECK_INTERVAL, "PT30M"))
                .build();

        Assertions.assertEquals(Duration.ofMinutes(30),
                config.getValue(ConsulClientConfiguration.PROP_AUTH_FALLBACK_RECHECK_INTERVAL, Duration.class));
    }
}
