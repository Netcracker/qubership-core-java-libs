package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class LocalDevTokenSourceTest {

    @SystemStub
    private SystemProperties systemProperties;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @AfterEach
    void clear() {
        System.clearProperty(LocalDevMode.ENABLED_PROPERTY);
        System.clearProperty(LocalDevMode.MICROSERVICE_NAME_PROPERTY);
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
    }

    @Test
    void delegatesToFallbackWhenDisabled() throws Exception {
        TokenSource fallback = mock(TokenSource.class);
        when(fallback.getToken("netcracker")).thenReturn("file-token");
        TokenRequestClient client = mock(TokenRequestClient.class);

        try (LocalDevTokenSource source = new LocalDevTokenSource(fallback, () -> client)) {
            assertEquals("file-token", source.getToken("netcracker"));
        }
        verify(fallback).getToken("netcracker");
    }

    @Test
    void requestsAndCachesTokenWhenEnabled() throws Exception {
        systemProperties.set(LocalDevMode.ENABLED_PROPERTY, "true");
        systemProperties.set(LocalDevMode.MICROSERVICE_NAME_PROPERTY, "my-sa");
        environmentVariables.set(LocalDevMode.NAMESPACE_ENV, "my-ns");

        TokenSource fallback = mock(TokenSource.class);
        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken(eq("my-ns"), eq("my-sa"), eq("netcracker")))
                .thenReturn(new TokenRequestClient.TokenRequestResult(
                        "minted", Instant.now().plusSeconds(3600)));

        AtomicInteger supplierCalls = new AtomicInteger();
        try (LocalDevTokenSource source = new LocalDevTokenSource(fallback, () -> {
            supplierCalls.incrementAndGet();
            return client;
        })) {
            assertEquals("minted", source.getToken("netcracker"));
            assertEquals("minted", source.getToken("netcracker"));
        }

        verify(client, times(1)).requestToken(eq("my-ns"), eq("my-sa"), eq("netcracker"));
        assertEquals(1, supplierCalls.get());
    }
}
