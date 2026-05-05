package com.netcracker.cloud.security.core.utils.k8s.impl;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

public final class M2MInterceptor implements Interceptor {

    private final M2MAuthenticator authenticator;

    public M2MInterceptor(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier) {
        this.authenticator = new M2MAuthenticator(urlCache, fallbackAuthHeaderSupplier, k8sAuthHeaderSupplier);
    }

    public M2MInterceptor(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier, String fallbackBaseUrl) {
        this.authenticator = new M2MAuthenticator(urlCache, fallbackAuthHeaderSupplier, k8sAuthHeaderSupplier, fallbackBaseUrl);
    }

    @NotNull
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        return authenticator.execute(
                request.url().uri(),
                (targetUrl, authHeader) -> chain.proceed(buildRequest(request, targetUrl, authHeader)),
                response -> response.code() == 401,
                Response::isSuccessful,
                Response::close
        );
    }

    private static Request buildRequest(Request original, URI targetUrl, String authHeader) {
        return original.newBuilder()
                .url(Objects.requireNonNull(HttpUrl.get(targetUrl)))
                .header("Authorization", authHeader)
                .build();
    }
}
