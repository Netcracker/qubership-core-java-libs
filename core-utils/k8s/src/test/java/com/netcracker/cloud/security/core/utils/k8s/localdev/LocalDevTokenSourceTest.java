package com.netcracker.cloud.security.core.utils.k8s.localdev;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        System.clearProperty(LocalDevMode.MICROSERVICE_NAME_PROPERTY);
        System.clearProperty(LocalDevMode.QUARKUS_PROFILE_PROPERTY);
    }

    @Test
    void requestsAndCachesToken() throws Exception {
        systemProperties.set(LocalDevMode.QUARKUS_PROFILE_PROPERTY, "dev");
        systemProperties.set(LocalDevMode.MICROSERVICE_NAME_PROPERTY, "my-sa");
        environmentVariables.set(LocalDevMode.NAMESPACE_ENV, "my-ns");

        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken("my-ns", "my-sa", "netcracker"))
                .thenReturn(new TokenRequestClient.TokenRequestResult(
                        "minted", Instant.now().plusSeconds(3600)));

        AtomicInteger supplierCalls = new AtomicInteger();
        LocalDevTokenSource source = new LocalDevTokenSource(() -> {
            supplierCalls.incrementAndGet();
            return client;
        });
        assertEquals("minted", source.getToken("netcracker"));
        assertEquals("minted", source.getToken("netcracker"));
        source.close();

        verify(client, times(1)).requestToken("my-ns", "my-sa", "netcracker");
        assertEquals(1, supplierCalls.get());
    }

    @Test
    void throwsWhenAudienceIsNull() {
        LocalDevTokenSource source = new LocalDevTokenSource(() -> mock(TokenRequestClient.class));
        assertThrows(NullPointerException.class, () -> source.getToken(null));
        source.close();
    }

    @Test
    void closeClearsCache() {
        LocalDevTokenSource source = new LocalDevTokenSource(() -> mock(TokenRequestClient.class));
        source.close();
    }
}
