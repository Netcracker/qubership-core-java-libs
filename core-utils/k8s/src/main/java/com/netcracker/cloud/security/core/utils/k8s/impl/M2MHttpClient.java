package com.netcracker.cloud.security.core.utils.k8s.impl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class M2MHttpClient extends HttpClient {

    private final HttpClient delegate;
    private final M2MAuthenticator authenticator;

    public M2MHttpClient(HttpClient delegate, M2MAuthenticator authenticator) {
        this.delegate = delegate;
        this.authenticator = authenticator;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        try {
            return authenticator.execute(
                    request.uri(),
                    (targetUrl, authHeader) -> sendWithDelegate(buildRequest(request, targetUrl, authHeader), responseBodyHandler),
                    response -> response.statusCode() == 401,
                    response -> response.statusCode() >= 200 && response.statusCode() < 300,
                    response -> { }
            );
        } catch (IOException e) {
            if (e.getCause() instanceof InterruptedException ie) {
                throw ie;
            }
            throw e;
        }
    }

    private <T> HttpResponse<T> sendWithDelegate(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return delegate.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during HTTP request", e);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(request, responseBodyHandler);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(e);
            }
        }, executor().orElse(Runnable::run));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    private static HttpRequest buildRequest(HttpRequest original, URI targetUrl, String authHeader) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUrl)
                .method(original.method(), original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        original.headers().map().forEach((name, values) -> {
            for (String value : values) {
                builder.header(name, value);
            }
        });
        builder.header("Authorization", authHeader);
        original.timeout().ifPresent(builder::timeout);
        return builder.build();
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
