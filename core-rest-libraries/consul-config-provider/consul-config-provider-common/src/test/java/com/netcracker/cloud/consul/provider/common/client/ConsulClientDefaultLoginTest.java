package com.netcracker.cloud.consul.provider.common.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsulClientDefaultLoginTest {

    private static class ExternalConsulClient implements ConsulClient {

        private String requestedAuthMethod;

        @Override
        public ConsulClientResponse getSelfToken(String currentSecretId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConsulClientResponse login(String authMethod) {
            requestedAuthMethod = authMethod;
            return new ConsulClientResponse("{\"SecretID\": \"test-secret-id\"}", 200);
        }
    }

    private static ConsulLoginCredentials credentials(String authMethod, String bearerToken) {
        return new ConsulLoginCredentials() {
            @Override
            public String getAuthMethod() {
                return authMethod;
            }

            @Override
            public String getBearerToken() {
                return bearerToken;
            }
        };
    }

    @Test
    void externalImplementationKeepsWorkingThroughNewMethod() throws IOException {
        ExternalConsulClient client = new ExternalConsulClient();

        ConsulClientResponse response = client.login(credentials("test-auth-method", "test-bearer-token"));

        assertEquals(200, response.getCode());
        assertEquals("{\"SecretID\": \"test-secret-id\"}", response.getBodyJson());
    }

    @Test
    void authMethodOfCredentialsGoesToOldMethod() throws IOException {
        ExternalConsulClient client = new ExternalConsulClient();

        client.login(credentials("test-auth-method", "test-bearer-token"));

        assertEquals("test-auth-method", client.requestedAuthMethod);
    }
}
