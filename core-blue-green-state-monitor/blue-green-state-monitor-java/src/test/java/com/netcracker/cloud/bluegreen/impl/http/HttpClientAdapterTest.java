package com.netcracker.cloud.bluegreen.impl.http;

import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientAdapterTest {

    private static final String KEY_URI = "http://consul-test:8500/v1/kv/key";

    private final List<String> reported = new ArrayList<>();

    @Test
    void aRefusedTokenIsReportedWithTheValueThatWasSent() throws IOException, InterruptedException {
        HttpClientAdapter adapter = new HttpClientAdapter(clientAnswering(403), () -> "dead-token", reported::add);

        assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertEquals(List.of("dead-token"), reported);
    }

    @Test
    void anErrorOtherThanARefusalIsNotReported() throws IOException, InterruptedException {
        HttpClientAdapter adapter = new HttpClientAdapter(clientAnswering(500), () -> "live-token", reported::add);

        assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertTrue(reported.isEmpty(), "reported tokens");
    }

    @Test
    void aRefusalTheCallerCountsAsSuccessIsNotReported() throws IOException, InterruptedException {
        HttpClientAdapter adapter = new HttpClientAdapter(clientAnswering(403), () -> "live-token", reported::add);

        adapter.invoke(request -> request.uri(URI.create(KEY_URI)).GET(), String.class, 403).send();

        assertTrue(reported.isEmpty(), "reported tokens");
    }

    /**
     * The token is read once per invocation, so a supplier that hands out a new one each time still reports the token
     * the request carried rather than the next one.
     */
    @Test
    void theReportedTokenIsTheOneTheRequestCarried() throws IOException, InterruptedException {
        Iterator<String> tokens = List.of("token-1", "token-2").iterator();
        HttpClientAdapter adapter = new HttpClientAdapter(clientAnswering(403), tokens::next, reported::add);

        assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertEquals(List.of("token-1"), reported);
    }

    @Test
    void anAdapterBuiltWithoutAConsumerPassesTheRefusalToItsCaller() throws IOException, InterruptedException {
        HttpClientAdapter adapter = new HttpClientAdapter(clientAnswering(403), () -> "dead-token");

        DefaultErrorCodeException failure = assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertEquals(403, failure.getHttpCode());
    }

    private static ResponseHandler<String> read(HttpClientAdapter adapter) {
        return adapter.invoke(request -> request.uri(URI.create(KEY_URI)).GET(), String.class).send();
    }

    @SuppressWarnings("unchecked")
    private static HttpClient clientAnswering(int statusCode) throws IOException, InterruptedException {
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(statusCode);
        Mockito.when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        Mockito.when(response.body()).thenReturn("");
        HttpClient client = Mockito.mock(HttpClient.class);
        Mockito.when(client.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(response);
        return client;
    }
}
