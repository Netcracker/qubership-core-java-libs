package com.netcracker.cloud.bluegreen.impl.http;

import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HttpClientAdapter {

    private final HttpClient client;
    private final Supplier<String> consulTokenSupplier;
    private final Consumer<String> onTokenRejected;

    public HttpClientAdapter(Supplier<String> consulTokenSupplier) {
        this(HttpClient.newHttpClient(), consulTokenSupplier);
    }

    public HttpClientAdapter(Supplier<String> consulTokenSupplier, Consumer<String> onTokenRejected) {
        this(HttpClient.newHttpClient(), consulTokenSupplier, onTokenRejected);
    }

    public HttpClientAdapter(HttpClient client, Supplier<String> consulTokenSupplier) {
        this(client, consulTokenSupplier, token -> {
        });
    }

    /**
     * @param onTokenRejected receives the token an invocation sent whenever Consul answers {@code 403 ACL not found},
     *                        so that the owner of the token can obtain a new one. A token Consul still resolves is
     *                        never passed
     */
    public HttpClientAdapter(HttpClient client, Supplier<String> consulTokenSupplier, Consumer<String> onTokenRejected) {
        this.client = client;
        this.consulTokenSupplier = consulTokenSupplier;
        this.onTokenRejected = onTokenRejected;
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, Class<T> type, int... successCodes) {
        String token = consulTokenSupplier.get();
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder, token), token, onTokenRejected, successCodes);
    }

    public <T> HttpInvocation<T> invoke(Consumer<HttpRequest.Builder> httpRequestBuilder, TypeReference<T> type, int... successCodes) {
        String token = consulTokenSupplier.get();
        return new HttpInvocation<>(type, client, buildRequest(httpRequestBuilder, token), token, onTokenRejected, successCodes);
    }

    private HttpRequest buildRequest(Consumer<HttpRequest.Builder> httpRequestBuilder, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + token);
        httpRequestBuilder.accept(builder);
        return builder.build();
    }
}
