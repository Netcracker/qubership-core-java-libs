package com.netcracker.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netcracker.cloud.consul.provider.common.client.ConsulClient.*;

class ConsulRestClientTest {

    @Test
    void getSelfTokenSuccess() {
        String currentSecretId = "my-current-secret-id";
        String consulAddress = "consul:8301";
        MicroserviceRestClient restClient = Mockito.mock(MicroserviceRestClient.class);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put(X_CONSUL_TOKEN_HEADER, Collections.singletonList(currentSecretId));

        Mockito.when(restClient.doRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(String.class)))
                .thenReturn(new RestClientResponseEntity<>("", 200));

        ConsulClient consulClient = new ConsulRestClient(restClient, consulAddress, () -> "");
        consulClient.getSelfToken(currentSecretId);

        Mockito.verify(restClient, Mockito.times(1))
                .doRequest(Mockito.eq(consulAddress + V1_ACL_TOKEN_SELF), Mockito.eq(HttpMethod.GET),
                        Mockito.eq(headers), Mockito.any(), Mockito.eq(String.class));
    }

    @Test
    void loginSuccess() {
        String authMethod = "core-ci";
        String m2m = "my-secret-m2m-token";
        String consulAddress = "consul:8301";
        MicroserviceRestClient restClient = Mockito.mock(MicroserviceRestClient.class);

        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, authMethod);
        payload.put(BEARER_TOKEN_FIELD, m2m);
        String jsonPayload = new Gson().toJson(payload);

        RestClientResponseEntity<String> expectedResponse = new RestClientResponseEntity<>(jsonPayload, 200);
        Mockito.when(restClient.doRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(String.class)))
                .thenReturn(expectedResponse);

        ConsulClient consulClient = new ConsulRestClient(restClient, consulAddress, () -> m2m);
        ConsulClientResponse consulResponse = consulClient.login(authMethod);

        Assertions.assertEquals(200, consulResponse.getCode());
        Assertions.assertEquals(jsonPayload, consulResponse.getBodyJson());
        Mockito.verify(restClient, Mockito.times(1)).doRequest(
                Mockito.eq(consulAddress + V1_ACL_LOGIN), Mockito.eq(HttpMethod.POST), Mockito.any(),
                Mockito.eq(jsonPayload), Mockito.eq(String.class));
    }

    private static ConsulLoginCredentials credentials(String authMethod, String bearerToken) {
        return new ConsulLoginCredentials() {
            @Override
            public String authMethod() {
                return authMethod;
            }

            @Override
            public String bearerToken() {
                return bearerToken;
            }
        };
    }

    @Test
    void loginByCredentialsWrapsTransportFailureInIOException() {
        String consulAddress = "consul:8301";
        MicroserviceRestClient restClient = Mockito.mock(MicroserviceRestClient.class);

        MicroserviceRestClientException cause = new MicroserviceRestClientException("consul is unreachable");
        Mockito.when(restClient.doRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(String.class)))
                .thenThrow(cause);

        ConsulClient consulClient = new ConsulRestClient(restClient, consulAddress, () -> "");

        IOException thrown = Assertions.assertThrows(IOException.class,
                () -> consulClient.login(credentials("core-k8s", "my-secret-bearer-token")));
        Assertions.assertSame(cause, thrown.getCause());
    }

    @Test
    void loginByCredentialsSendsAuthMethodAndBearerTokenFromCredentials() throws IOException {
        String authMethod = "core-k8s";
        String bearerToken = "my-secret-bearer-token";
        String consulAddress = "consul:8301";
        MicroserviceRestClient restClient = Mockito.mock(MicroserviceRestClient.class);

        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, authMethod);
        payload.put(BEARER_TOKEN_FIELD, bearerToken);
        String jsonPayload = new Gson().toJson(payload);

        Mockito.when(restClient.doRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(String.class)))
                .thenReturn(new RestClientResponseEntity<>(jsonPayload, 200));

        ConsulClient consulClient = new ConsulRestClient(restClient, consulAddress, () -> "unused-m2m-token");
        ConsulClientResponse consulResponse = consulClient.login(credentials(authMethod, bearerToken));

        Assertions.assertEquals(200, consulResponse.getCode());
        Mockito.verify(restClient, Mockito.times(1)).doRequest(
                Mockito.eq(consulAddress + V1_ACL_LOGIN), Mockito.eq(HttpMethod.POST), Mockito.any(),
                Mockito.eq(jsonPayload), Mockito.eq(String.class));
    }
}
