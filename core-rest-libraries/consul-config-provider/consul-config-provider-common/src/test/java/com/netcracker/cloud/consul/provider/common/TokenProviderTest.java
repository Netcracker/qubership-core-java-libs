package com.netcracker.cloud.consul.provider.common;

import com.jayway.jsonpath.PathNotFoundException;
import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenProviderTest {

    private static final String LOGIN_RESPONSE_WITHOUT_EXPIRATION =
            "{\"AccessorID\":\"test-accessor-id\",\"SecretID\":\"test-secret-id\",\"Description\":\"token created via login\"," +
                    "\"Roles\":[{\"ID\":\"test-role-id\",\"Name\":\"poc-reader\"}],\"Local\":true,\"AuthMethod\":\"k8s-poc\"," +
                    "\"CreateTime\":\"2026-08-26T07:21:18.613036445Z\",\"Hash\":\"test-hash\",\"CreateIndex\":52,\"ModifyIndex\":52}";

    private static final String LOGIN_RESPONSE_WITH_EXPIRATION =
            "{\"AccessorID\":\"test-accessor-id\",\"SecretID\":\"test-secret-id\",\"Description\":\"token created via login\"," +
                    "\"Roles\":[{\"ID\":\"test-role-id\",\"Name\":\"poc-reader\"}],\"Local\":true,\"AuthMethod\":\"k8s-poc-ttl\"," +
                    "\"ExpirationTime\":\"2026-08-26T07:26:30.522472777Z\",\"CreateTime\":\"2026-08-26T07:21:30.522472777Z\"," +
                    "\"Hash\":\"test-hash\",\"CreateIndex\":53,\"ModifyIndex\":53}";

    private ConsulClient consulClient;
    private ConsulLoginCredentials credentials;
    private TokenProvider tokenProvider;

    @BeforeEach
    public void init() {
        consulClient = mock(ConsulClient.class);
        credentials = new M2MLoginCredentials("test", () -> "test-m2m-token");
        tokenProvider = new TokenProvider(consulClient, credentials);
    }

    @Test
    void performReadsSecretIdAndExpirationTime() throws IOException {
        when(consulClient.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse(LOGIN_RESPONSE_WITH_EXPIRATION, 200));

        Token token = tokenProvider.perform();

        verify(consulClient).login(eq(credentials));
        assertEquals("test-secret-id", token.getSecretId());
        assertEquals(OffsetDateTime.parse("2026-08-26T07:26:30.522472777Z"), token.getExpirationTime());
    }

    @Test
    void performReadsResponseWithoutExpirationTime() throws IOException {
        when(consulClient.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse(LOGIN_RESPONSE_WITHOUT_EXPIRATION, 200));

        Token token = tokenProvider.perform();

        assertEquals("test-secret-id", token.getSecretId());
        assertNull(token.getExpirationTime());
    }

    @Test
    void performFailsWithIOExceptionOnEmptyBody() throws IOException {
        when(consulClient.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("", 200));

        assertThrows(IOException.class, () -> tokenProvider.perform());
    }

    @Test
    void performFailsWithIOExceptionOnNonSuccessCode() throws IOException {
        when(consulClient.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("{\"Error\":\"Permission denied\"}", 403));

        assertThrows(IOException.class, () -> tokenProvider.perform());
    }

    @Test
    void performFailsWithPathNotFoundExceptionWhenSecretIdIsMissing() throws IOException {
        when(consulClient.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("{\"AccessorID\":\"test-accessor-id\"}", 200));

        assertThrows(PathNotFoundException.class, () -> tokenProvider.perform());
        verify(consulClient, times(1)).login(any(ConsulLoginCredentials.class));
    }
}
