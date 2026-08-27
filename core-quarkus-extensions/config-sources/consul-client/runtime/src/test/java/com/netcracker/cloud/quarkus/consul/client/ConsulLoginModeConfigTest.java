package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ConsulLoginModeConfigTest {

    private static SmallRyeConfig configWithMode(String mode) {
        return new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of(ConsulClientConfiguration.PROP_LOGIN_MODE, mode))
                .build();
    }

    private static ConsulLoginMode read(String mode) {
        return configWithMode(mode).getValue(ConsulClientConfiguration.PROP_LOGIN_MODE, ConsulLoginMode.class);
    }

    @Test
    void everyModeIsReadFromItsPropertyValue() {
        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, read("kubernetes"));
        Assertions.assertEquals(ConsulLoginMode.M2M, read("m2m"));
        Assertions.assertEquals(ConsulLoginMode.KUBERNETES_WITH_M2M_FALLBACK, read("kubernetes-with-m2m-fallback"));
    }

    @Test
    void unknownModeBreaksTheStart() {
        SmallRyeConfig config = configWithMode("cloud-foundry");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> config.getValue(ConsulClientConfiguration.PROP_LOGIN_MODE, ConsulLoginMode.class));
    }
}
