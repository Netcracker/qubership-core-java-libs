package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.consul.provider.common.TokenStorageFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestProfile(ConsulClientConfigurationTest.Profile.class)
class ConsulClientConfigurationTest {

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "cloud.microservice.name", "test-app",
                    "cloud.microservice.namespace", "test-namespace",
                    "quarkus.consul-source-config.enabled", "false",
                    "quarkus.consul-source-config.agent.url", "http://localhost:8500"
            );
        }
    }

    @InjectMock
    TokenStorageFactory tokenStorageFactory;

    @Inject
    TokenStorage tokenStorage;

    @Test
    void test() {
        Assert.assertNotNull(tokenStorage);
        verify(tokenStorageFactory, never()).create(any());
    }
}