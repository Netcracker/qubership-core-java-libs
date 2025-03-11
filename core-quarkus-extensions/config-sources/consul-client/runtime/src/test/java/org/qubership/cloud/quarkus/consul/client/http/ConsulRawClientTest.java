package org.qubership.cloud.quarkus.consul.client.http;

import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsulRawClientTest {

    private HttpTransport httpTransport;
    private ConsulRawClient consulRawClient;
    private String consulUrl = "localhost:8500";

    @BeforeEach
    void setUp() {
        httpTransport = mock(HttpTransport.class);
        consulRawClient = new ConsulRawClient(httpTransport, consulUrl);
    }

    @Test
    void testMakeGetRequest_success() {
        String endpoint = "/v1/kv/test";
        QueryParams queryParams = new QueryParams(-1, -1);

        Response<List<GetValue>> expectedResponse = new Response<>(null, 0L, true, 0L);
        when(httpTransport.makeGetRequest(Mockito.anyString())).thenReturn(expectedResponse);

        Response<List<GetValue>> actualResponse = consulRawClient.makeGetRequest(endpoint, queryParams);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void testGenerateUrl() {
        String baseUrl = "http://localhost:8500/v1/kv/test";
        QueryParams queryParams = new QueryParams(10, 100);
        queryParams.setToken("test-token");

        String generatedUrl = ConsulRawClient.generateUrl(baseUrl, queryParams);

        assertTrue(generatedUrl.contains("wait=10s"));
        assertTrue(generatedUrl.contains("index=100"));
        assertTrue(generatedUrl.contains("token=test-token"));
    }
}