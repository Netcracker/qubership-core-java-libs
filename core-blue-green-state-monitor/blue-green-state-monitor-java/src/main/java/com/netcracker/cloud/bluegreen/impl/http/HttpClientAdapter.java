package com.netcracker.cloud.bluegreen.impl.http;

import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HttpClientAdapter {

    private final HttpClient client;
    private final Supplier<String> consulTokenSupplier;
    private final Runnable onRefusal;

    public HttpClientAdapter(Supplier<String> consulTokenSupplier) {
        this(HttpClient.newHttpClient(), consulTokenSupplier);
    }

    public HttpClientAdapter(Supplier<String> consulTokenSupplier, Runnable onRefusal) {
        this(HttpClient.newHttpClient(), consulTokenSupplier, onRefusal);
    }

    public HttpClientAdapter(HttpClient client, Supplier<String> consulTokenSupplier) {
        this(client, consulTokenSupplier, () -> {
        });
    }

    /**
     * @param onRefusal runs whenever Consul answers {@code 403} to an invocation of this adapter, so that the owner
     *                  of the token can check whether Consul still resolves it
     */
    public HttpClientAdapter(HttpClient client, Supplier<String> consulTokenSupplier, Runnable onRefusal) {
        this.client = client;
        this.consulTokenSupplier = consulTokenSupplier;
        this.onRefusal = onRefusal;
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, Class<T> type, int... successCodes) {
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder), onRefusal, successCodes);
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, TypeReference<T> type, int... successCodes) {
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder), onRefusal, successCodes);
    }

    private HttpRequest buildRequest(Consumer<HttpRequest.Builder> httpRequestBuilder) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + consulTokenSupplier.get());
        httpRequestBuilder.accept(builder);
        return builder.build();
    }
}
