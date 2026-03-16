package com.netcracker.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
}
