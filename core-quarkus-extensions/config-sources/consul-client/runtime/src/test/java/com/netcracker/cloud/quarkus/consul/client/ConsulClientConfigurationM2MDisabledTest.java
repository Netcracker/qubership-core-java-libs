package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
@TestProfile(ConsulClientConfigurationM2MDisabledTest.Profile.class)
class ConsulClientConfigurationM2MDisabledTest {

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "cloud.microservice.name", "test-app",
                    "cloud.microservice.namespace", "test-namespace",
                    "quarkus.consul-source-config.enabled", "false",
                    "quarkus.consul-source-config.agent.url", "http://localhost:8500",
                    "quarkus.consul-source-config.m2m.enabled", "false",
                    ConsulClientConfiguration.PROP_LOGIN_MODE, "cloud-foundry"
            );
        }
    }

    @Inject
    TokenStorage tokenStorage;

    @Test
    void disabledM2MKeepsTheStubAndReadsNoLoginProperties() {
        Assertions.assertEquals("", tokenStorage.get());
    }
}
