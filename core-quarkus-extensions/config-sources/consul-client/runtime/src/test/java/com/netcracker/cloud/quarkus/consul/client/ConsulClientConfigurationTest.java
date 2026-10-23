package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.common.Assert;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    /**
     * The producer would take the mode from the {@code M2M_AUTH_MODE} of the machine running the test, so the variable
     * is removed around the first call of the bean, where the producer runs.
     */
    @Test
    void defaultsAreTakenWhenNoLoginPropertyIsSet() throws Exception {
        when(tokenStorageFactory.create(any())).thenReturn(new NoopTokenStorage());

        new EnvironmentVariables().remove(M2MAuthMode.M2M_AUTH_MODE_ENV).execute(() -> tokenStorage.get());

        ArgumentCaptor<TokenStorageFactory.CreateOptions> options =
                ArgumentCaptor.forClass(TokenStorageFactory.CreateOptions.class);
        verify(tokenStorageFactory).create(options.capture());
        Assertions.assertEquals(ConsulLoginMode.LEGACY, options.getValue().getMode());
        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_AUTH_METHOD, options.getValue().getAuthMethod());
        Assertions.assertEquals(AudienceName.NETCRACKER, options.getValue().getAudience());
    }

    static class NoopTokenStorage implements TokenStorage {

        @Override
        public String get() {
            return "";
        }

        @Override
        public void update(String token) {
            // nothing
        }
    }
}
