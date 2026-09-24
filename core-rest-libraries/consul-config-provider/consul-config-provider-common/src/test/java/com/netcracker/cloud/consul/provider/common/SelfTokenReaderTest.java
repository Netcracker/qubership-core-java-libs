package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfTokenReaderTest {

    private static final String SELF_RESPONSE_WITH_EXPIRATION =
            "{\"AccessorID\":\"test-accessor-id\",\"SecretID\":\"test-secret-id\",\"Description\":\"token created via login\"," +
                    "\"Local\":true,\"AuthMethod\":\"k8s-poc-ttl\",\"ExpirationTime\":\"2026-08-26T07:26:30.522472777Z\"," +
                    "\"CreateTime\":\"2026-08-26T07:21:30.522472777Z\",\"Hash\":\"test-hash\",\"CreateIndex\":53,\"ModifyIndex\":53}";

    private static final String SELF_RESPONSE_WITHOUT_EXPIRATION =
            "{\"AccessorID\":\"test-accessor-id\",\"SecretID\":\"test-secret-id\",\"Description\":\"token created via login\"," +
                    "\"Local\":true,\"AuthMethod\":\"k8s-poc\",\"CreateTime\":\"2026-08-26T07:21:18.613036445Z\"," +
                    "\"Hash\":\"test-hash\",\"CreateIndex\":52,\"ModifyIndex\":52}";

    private static final String SELF_RESPONSE_WITHOUT_AUTH_METHOD =
            "{\"AccessorID\":\"test-accessor-id\",\"SecretID\":\"test-secret-id\",\"Description\":\"static token\"," +
                    "\"Local\":true,\"CreateTime\":\"2026-08-26T07:21:18.613036445Z\"," +
                    "\"Hash\":\"test-hash\",\"CreateIndex\":52,\"ModifyIndex\":52}";

    private ConsulClient consulClient;
    private SelfTokenReader selfTokenReader;

    @BeforeEach
    public void init() {
        consulClient = mock(ConsulClient.class);
        selfTokenReader = new SelfTokenReader(consulClient);
    }

    @Test
    void readReturnsSecretIdAndExpirationTime() throws IOException {
        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse(SELF_RESPONSE_WITH_EXPIRATION, 200));

        Token token = selfTokenReader.read("test-current-secret-id");

        verify(consulClient).getSelfToken(eq("test-current-secret-id"));
        assertEquals("test-secret-id", token.getSecretId());
        assertEquals(OffsetDateTime.parse("2026-08-26T07:26:30.522472777Z"), token.getExpirationTime());
    }

    @Test
    void readSurvivesMissingExpirationTime() throws IOException {
        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse(SELF_RESPONSE_WITHOUT_EXPIRATION, 200));

        Token token = selfTokenReader.read("test-current-secret-id");

        assertEquals("test-secret-id", token.getSecretId());
        assertNull(token.getExpirationTime());
    }

    @Test
    void readReportsTheAuthMethodThatIssuedTheToken() throws IOException {
        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse(SELF_RESPONSE_WITH_EXPIRATION, 200));

        assertEquals("k8s-poc-ttl", selfTokenReader.read("test-current-secret-id").getAuthMethod());
    }

    @Test
    void readSurvivesMissingAuthMethod() throws IOException {
        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse(SELF_RESPONSE_WITHOUT_AUTH_METHOD, 200));

        Token token = selfTokenReader.read("test-current-secret-id");

        assertEquals("test-secret-id", token.getSecretId());
        assertNull(token.getAuthMethod());
    }

    @Test
    void readFailsWithIOExceptionOnNonSuccessCode() {
        when(consulClient.getSelfToken(anyString()))
                .thenReturn(new ConsulClientResponse("token does not exist: ACL not found", 403));

        assertThrows(IOException.class, () -> selfTokenReader.read("test-current-secret-id"));
    }

    @Test
    void readFailsWithIOExceptionOnEmptyBody() {
        when(consulClient.getSelfToken(anyString())).thenReturn(new ConsulClientResponse(null, 200));

        assertThrows(IOException.class, () -> selfTokenReader.read("test-current-secret-id"));

        when(consulClient.getSelfToken(anyString())).thenReturn(new ConsulClientResponse("", 200));

        assertThrows(IOException.class, () -> selfTokenReader.read("test-current-secret-id"));
    }
}
