package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The {@code TokenStorage} producer runs on the first call of the bean, not at startup, so the variable only has to be
 * set around that call. The stub is applied in place rather than through its JUnit extension because a Quarkus test
 * runs on an instance of its own.
 */
@QuarkusTest
@TestProfile(ConsulClientConfigurationM2MAuthModeTest.Profile.class)
class ConsulClientConfigurationM2MAuthModeTest {

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
    void modeIsTakenFromM2MAuthModeWhenConsulAuthModeIsUnset() throws Exception {
        when(tokenStorageFactory.create(any())).thenReturn(new ConsulClientConfigurationTest.NoopTokenStorage());

        new EnvironmentVariables(M2MAuthMode.M2M_AUTH_MODE_ENV, "k8s").execute(() -> tokenStorage.get());

        ArgumentCaptor<TokenStorageFactory.CreateOptions> options =
                ArgumentCaptor.forClass(TokenStorageFactory.CreateOptions.class);
        verify(tokenStorageFactory).create(options.capture());
        Assertions.assertEquals(ConsulLoginMode.K8S, options.getValue().getMode());
    }
}
