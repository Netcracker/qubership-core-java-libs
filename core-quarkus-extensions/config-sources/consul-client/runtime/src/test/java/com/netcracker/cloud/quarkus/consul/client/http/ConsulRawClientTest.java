package com.netcracker.cloud.quarkus.consul.client.http;

import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void aRefusalIsReported() {
        AtomicInteger reported = new AtomicInteger();
        ConsulRawClient client = new ConsulRawClient(httpTransport, consulUrl, reported::incrementAndGet);
        when(httpTransport.makeGetRequestAsync(Mockito.anyString(), Mockito.eq(new String[]{"Authorization", "Bearer dead-token"})))
                .thenReturn(answerWith(403));

        assertThrows(CompletionException.class,
                () -> client.makeGetRequest("/v1/kv/test", new QueryParams(-1, -1), "dead-token"));

        assertEquals(1, reported.get(), "reported refusals");
    }

    @Test
    void anErrorOtherThanARefusalIsNotReported() {
        AtomicInteger reported = new AtomicInteger();
        ConsulRawClient client = new ConsulRawClient(httpTransport, consulUrl, reported::incrementAndGet);
        when(httpTransport.makeGetRequestAsync(Mockito.anyString(), Mockito.eq(new String[]{"Authorization", "Bearer live-token"})))
                .thenReturn(answerWith(500));

        assertThrows(CompletionException.class,
                () -> client.makeGetRequest("/v1/kv/test", new QueryParams(-1, -1), "live-token"));

        assertEquals(0, reported.get(), "reported refusals");
    }

    @Test
    void testGenerateUrl() {
        String baseUrl = "http://localhost:8500/v1/kv/test";
        QueryParams queryParams = new QueryParams(10, 100);

        String generatedUrl = ConsulRawClient.generateUrl(baseUrl, queryParams);

        assertTrue(generatedUrl.contains("wait=10s"));
        assertTrue(generatedUrl.contains("index=100"));
    }

    /**
     * Fails the future the way {@link HttpTransport} does, so that the {@link OperationException} arrives wrapped in a
     * {@link CompletionException} as it does in production.
     */
    private static CompletableFuture<Response<List<GetValue>>> answerWith(int statusCode) {
        return CompletableFuture.<Response<List<GetValue>>>completedFuture(null)
                .thenApply(ignored -> {
                    throw new OperationException(statusCode, "An error occurred while executing the request", "");
                });
    }
}
