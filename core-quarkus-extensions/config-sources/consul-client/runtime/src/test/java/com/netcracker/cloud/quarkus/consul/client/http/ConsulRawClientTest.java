package com.netcracker.cloud.quarkus.consul.client.http;

import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(httpTransport.makeGetRequestAsync(Mockito.anyString(), Mockito.eq(new String[]{"Authorization", "Bearer test-token"})))
                .thenReturn(CompletableFuture.completedFuture(expectedResponse));

        Response<List<GetValue>> actualResponse = consulRawClient.makeGetRequest(endpoint, queryParams, "test-token");

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void testGenerateUrl() {
        String baseUrl = "http://localhost:8500/v1/kv/test";
        QueryParams queryParams = new QueryParams(10, 100);

        String generatedUrl = ConsulRawClient.generateUrl(baseUrl, queryParams);

        assertTrue(generatedUrl.contains("wait=10s"));
        assertTrue(generatedUrl.contains("index=100"));
    }
}
