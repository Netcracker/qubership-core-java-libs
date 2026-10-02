package com.netcracker.cloud.bluegreen.impl.http;

import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpClientAdapterTest {

    private static final String KEY_URI = "http://consul-test:8500/v1/kv/key";

    private final AtomicInteger reported = new AtomicInteger();

    @Test
    void aRefusalIsReported() throws IOException, InterruptedException {
        HttpClientAdapter adapter =
                new HttpClientAdapter(clientAnswering(403), () -> "dead-token", reported::incrementAndGet);

        assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertEquals(1, reported.get(), "reported refusals");
    }

    @Test
    void anErrorOtherThanARefusalIsNotReported() throws IOException, InterruptedException {
        HttpClientAdapter adapter =
                new HttpClientAdapter(clientAnswering(500), () -> "live-token", reported::incrementAndGet);

        assertThrows(DefaultErrorCodeException.class, () -> read(adapter));

        assertEquals(0, reported.get(), "reported refusals");
    }

    @Test
    void aRefusalTheCallerCountsAsSuccessIsNotReported() throws IOException, InterruptedException {
        HttpClientAdapter adapter =
                new HttpClientAdapter(clientAnswering(403), () -> "live-token", reported::incrementAndGet);

        adapter.invoke(request -> request.uri(URI.create(KEY_URI)).GET(), String.class, 403).send();

        assertEquals(0, reported.get(), "reported refusals");
    }

    @Test
    void anAdapterBuiltWithoutACallbackPassesTheRefusalToItsCaller() throws IOException, InterruptedException {
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
