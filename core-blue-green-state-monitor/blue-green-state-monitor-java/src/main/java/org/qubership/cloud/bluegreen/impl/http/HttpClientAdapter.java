package org.qubership.cloud.bluegreen.impl.http;

import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HttpClientAdapter {

    private final HttpClient client;
    private final Supplier<String> consulTokenSupplier;

    public HttpClientAdapter(Supplier<String> consulTokenSupplier) {
        this(HttpClient.newHttpClient(), consulTokenSupplier);
    }

    public HttpClientAdapter(HttpClient client, Supplier<String> consulTokenSupplier) {
        this.client = client;
        this.consulTokenSupplier = consulTokenSupplier;
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, Class<T> type, int... successCodes) {
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder), successCodes);
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, TypeReference<T> type, int... successCodes) {
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder), successCodes);
    }

    private HttpRequest buildRequest(Consumer<HttpRequest.Builder> httpRequestBuilder) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + consulTokenSupplier.get());
        httpRequestBuilder.accept(builder);
        return builder.build();
    }
}
