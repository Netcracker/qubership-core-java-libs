package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(ConsulClientConfigurationKubernetesModeTest.Profile.class)
class ConsulClientConfigurationKubernetesModeTest {

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "cloud.microservice.name", "test-app",
                    "cloud.microservice.namespace", "test-namespace",
                    "quarkus.consul-source-config.enabled", "false",
                    "quarkus.consul-source-config.agent.url", "http://localhost:8500",
                    ConsulClientConfiguration.PROP_LOGIN_MODE, "kubernetes",
                    ConsulClientConfiguration.PROP_LOGIN_AUTH_METHOD, "core-k8s",
                    ConsulClientConfiguration.PROP_LOGIN_AUDIENCE, "dbaas"
            );
        }
    }

    @InjectMock
    TokenStorageFactory tokenStorageFactory;

    @Inject
    TokenStorage tokenStorage;

    @Test
    void loginPropertiesReachCreateOptionsAtRuntime() {
        when(tokenStorageFactory.create(any())).thenReturn(new ConsulClientConfigurationTest.NoopTokenStorage());

        tokenStorage.get();

        ArgumentCaptor<TokenStorageFactory.CreateOptions> options =
                ArgumentCaptor.forClass(TokenStorageFactory.CreateOptions.class);
        verify(tokenStorageFactory).create(options.capture());
        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, options.getValue().getMode());
        Assertions.assertEquals("core-k8s", options.getValue().getAuthMethod());
        Assertions.assertEquals("dbaas", options.getValue().getAudience());
    }
}
