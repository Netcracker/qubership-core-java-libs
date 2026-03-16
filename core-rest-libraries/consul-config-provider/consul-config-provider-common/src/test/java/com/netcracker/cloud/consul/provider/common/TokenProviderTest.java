package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenProviderTest {

    private ConsulClient consulClient;
    private TokenProvider tokenProvider;

    @BeforeEach
    public void init() {
        consulClient = mock(ConsulClient.class);
        tokenProvider = new TokenProvider(consulClient, "test");
    }

    @Test
    void getSelfConsulToken() throws IOException {
        String secretId = "test-secret-id";
        String currentToken = "test-current-token-id";
        OffsetDateTime expirationTime = OffsetDateTime.now().plusMinutes(30);

        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse("{\"SecretID\": \"" + secretId + "\", \"ExpirationTime\": \"" + expirationTime + "\"}", 200));

        Token newToken = tokenProvider.getSelf(currentToken);
        assertEquals(secretId, newToken.getSecretId());
        assertEquals(expirationTime, newToken.getExpirationTime());
        verify(consulClient).getSelfToken(eq(currentToken));
    }

    @Test
    void getNewConsulToken() throws IOException {
        String secretId = "test-secret-id";
        OffsetDateTime expirationTime = OffsetDateTime.now().plusMinutes(30);

        when(consulClient.login(anyString()))
                .thenReturn(new ConsulClientResponse("{\"SecretID\": \"" + secretId + "\", \"ExpirationTime\": \"" + expirationTime + "\"}", 200));

        Token token = tokenProvider.getNewConsulToken();
        verify(consulClient).login("test");
        assertEquals(secretId, token.getSecretId());
        assertEquals(expirationTime.toEpochSecond(), token.getExpirationTime().toEpochSecond());
    }
}
