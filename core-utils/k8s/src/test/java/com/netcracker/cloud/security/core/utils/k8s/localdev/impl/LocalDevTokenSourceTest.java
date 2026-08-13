package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDevTokenSourceTest {

    @Test
    void requestsAndCachesToken() {
        TokenRequestClient client = mockClient("minted");
        LocalDevTokenSource source = new LocalDevTokenSource(client, "my-sa", "my-ns");

        assertEquals("minted", source.getToken("netcracker"));
        assertEquals("minted", source.getToken("netcracker"));
        source.close();

        verify(client, times(1)).requestToken("my-ns", "my-sa", "netcracker");
    }

    @Test
    void throwsWhenAudienceIsNull() {
        LocalDevTokenSource source = new LocalDevTokenSource(mock(TokenRequestClient.class), "my-sa", "my-ns");
        assertThrows(NullPointerException.class, () -> source.getToken(null));
        source.close();
    }

    @Test
    void closeClearsCache() {
        TokenRequestClient client = mockClient("minted");
        LocalDevTokenSource source = new LocalDevTokenSource(client, "my-sa", "my-ns");

        assertEquals("minted", source.getToken("netcracker"));
        source.close();
        assertEquals("minted", source.getToken("netcracker"));
        verify(client, times(2)).requestToken("my-ns", "my-sa", "netcracker");
        source.close();
    }

    @Test
    void failsWhenMicroserviceNameMissing() {
        LocalDevTokenSource source = new LocalDevTokenSource(mock(TokenRequestClient.class), null, "my-ns");
        assertThrows(IllegalStateException.class, () -> source.getToken("netcracker"));
        source.close();
    }

    @Test
    void failsWhenNamespaceMissing() {
        LocalDevTokenSource source = new LocalDevTokenSource(mock(TokenRequestClient.class), "my-sa", null);
        assertThrows(IllegalStateException.class, () -> source.getToken("netcracker"));
        source.close();
    }

    private static TokenRequestClient mockClient(String token) {
        TokenRequestClient client = mock(TokenRequestClient.class);
        when(client.requestToken("my-ns", "my-sa", "netcracker"))
                .thenReturn(new TokenRequestClient.TokenRequestResult(token, Instant.now().plusSeconds(3600)));
        return client;
    }
}
