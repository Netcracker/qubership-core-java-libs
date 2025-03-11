package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.quarkus.consul.client.http.ConsulRawClient;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsulClientTest {

    private ConsulRawClient rawClient;
    private ConsulClient consulClient;

    @BeforeEach
    void setUp() {
        rawClient = mock(ConsulRawClient.class);
        consulClient = new ConsulClient(rawClient);
    }

    @Test
    void testGetKVValues_success() {
        String keyPrefix = "test";
        String token = "test-token";
        QueryParams queryParams = new QueryParams(-1, -1);
        Response<List<GetValue>> expectedResponse = new Response<>(null, 0L, true, 0L);

        when(rawClient.makeGetRequest(anyString(), any(QueryParams.class))).thenReturn(expectedResponse);

        Response<List<GetValue>> actualResponse = consulClient.getKVValues(keyPrefix, token, queryParams);

        assertEquals(expectedResponse, actualResponse);
        verify(rawClient, times(1)).makeGetRequest(anyString(), any(QueryParams.class));
    }
}

