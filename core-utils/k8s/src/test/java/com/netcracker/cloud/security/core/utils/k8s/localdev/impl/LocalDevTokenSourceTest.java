package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDevTokenSourceTest {

    @Test
    void requestsAndCachesToken() {
        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken("my-ns", "my-sa", "netcracker"))
                .thenReturn(new TokenRequestClient.TokenRequestResult(
                        "minted", Instant.now().plusSeconds(3600)));

        AtomicInteger supplierCalls = new AtomicInteger();
        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> {
                    supplierCalls.incrementAndGet();
                    return client;
                },
                () -> "my-sa",
                () -> "my-ns");
        assertEquals("minted", source.getToken("netcracker"));
        assertEquals("minted", source.getToken("netcracker"));
        source.close();

        verify(client, times(1)).requestToken("my-ns", "my-sa", "netcracker");
        assertEquals(1, supplierCalls.get());
    }

    @Test
    void throwsWhenAudienceIsNull() {
        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> mock(TokenRequestClient.class),
                () -> "my-sa",
                () -> "my-ns");
        assertThrows(NullPointerException.class, () -> source.getToken(null));
        source.close();
    }

    @Test
    void closeClearsCache() {
        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken("my-ns", "my-sa", "netcracker"))
                .thenReturn(new TokenRequestClient.TokenRequestResult(
                        "minted", Instant.now().plusSeconds(3600)));

        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> client,
                () -> "my-sa",
                () -> "my-ns");
        assertEquals("minted", source.getToken("netcracker"));
        source.close();
        assertEquals("minted", source.getToken("netcracker"));
        verify(client, times(2)).requestToken("my-ns", "my-sa", "netcracker");
        source.close();
    }

    @Test
    void failsWhenMicroserviceNameMissing() {
        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> mock(TokenRequestClient.class),
                () -> null,
                () -> "my-ns");
        assertThrows(IllegalStateException.class, () -> source.getToken("netcracker"));
        source.close();
    }

    @Test
    void failsWhenNamespaceMissing() {
        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> mock(TokenRequestClient.class),
                () -> "my-sa",
                () -> null);
        assertThrows(IllegalStateException.class, () -> source.getToken("netcracker"));
        source.close();
    }

    @Test
    void resolvesMicroserviceNameFromSupplier() {
        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken("my-ns", "my-sa", "netcracker"))
                .thenReturn(new TokenRequestClient.TokenRequestResult(
                        "minted", Instant.now().plusSeconds(3600)));

        LocalDevTokenSource source = new LocalDevTokenSource(
                () -> client,
                () -> "my-sa",
                () -> "my-ns");
        assertEquals("minted", source.getToken("netcracker"));
        source.close();
    }
}
