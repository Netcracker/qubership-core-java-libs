package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import static com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache.calculateCacheKey;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

@Slf4j
public final class M2MInterceptor implements Interceptor {

    public static final String KUBERNETES_TOKEN_ACQUISITION_ERROR = """
            Error acquiring kubernetes token for m2m communication.
            The current version of the security library expects a kubernetes token with the `netcracker` audience to be mounted in the deployment.
            if you do not intend to use a kubernetes token, set M2M_AUTH_MODE to legacy.
            otherwise, make sure that a kubernetes token with the `netcracker` audience is properly mounted.
            the previous authentication method will be used as a fallback.""";
    public static final String KUBERNETES_TOKEN_UNAUTHORIZED_ERROR = """
            Unauthorized access (http 401).
            During an m2m interaction attempt using a kubernetes token with the `netcracker` audience, a 401 error was received.
            The possible cause is an outdated version of the security library on the server side.
            The previous authentication method will be used as a fallback.""";

    private final M2MAuthMode mode;
    private final UrlCache urlCache;
    private final Supplier<String> fallbackAuthHeaderSupplier;
    private final Supplier<String> k8sAuthHeaderSupplier;
    private final HttpUrl fallbackBaseUrl;

    public M2MInterceptor(M2MAuthMode mode, UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier) {
        this(mode, urlCache, fallbackAuthHeaderSupplier, k8sAuthHeaderSupplier, null);
    }

    public M2MInterceptor(M2MAuthMode mode, UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier, String fallbackBaseUrl) {
        this.mode = mode;
        this.urlCache = urlCache;
        this.fallbackAuthHeaderSupplier = fallbackAuthHeaderSupplier;
        this.k8sAuthHeaderSupplier = k8sAuthHeaderSupplier;
        this.fallbackBaseUrl = (fallbackBaseUrl != null) ? HttpUrl.get(fallbackBaseUrl) : null;
    }

    @NotNull
    @Override
    public Response intercept(final Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        return switch (mode) {
            case LEGACY -> proceedWithKeycloakToken(request, chain);
            case HYBRID -> proceedHybrid(request, chain);
            case K8S -> proceedWithKubernetesToken(request, chain);
        };
    }

    private Response proceedWithKubernetesToken(final Request request, final Interceptor.Chain chain) throws IOException {
        final Request altered = alterRequest(request, k8sAuthHeaderSupplier.get(), false);
        log.debug("Sending http request to {} using kubernetes token", altered.url());
        return chain.proceed(altered);
    }

    private Response proceedHybrid(final Request request, final Interceptor.Chain chain) throws IOException {
        final String cacheKey = calculateCacheKey(request.url().toString());
        if (urlCache.containsKey(cacheKey)) {
            return proceedWithKeycloakToken(request, chain);
        }
        //first call (no information) / kubernetes token is applicable
        final Request altered;
        try {
            altered = alterRequest(request, k8sAuthHeaderSupplier.get(), false);
            log.debug("Sending http request to {} using kubernetes token", altered.url());
        } catch (IllegalStateException|IllegalArgumentException ex) {
            final Request fallbackRequest = alterRequest(request, fallbackAuthHeaderSupplier.get(), true);
            return doRequestFallback(fallbackRequest, KUBERNETES_TOKEN_ACQUISITION_ERROR, cacheKey, chain);
        }
        final Response response = chain.proceed(altered);
        if (response.code() == HTTP_UNAUTHORIZED) {
            //authentication failed, need to use old approach
            response.close();
            final Request fallbackRequest = alterRequest(request, fallbackAuthHeaderSupplier.get(), true);
            return doRequestFallback(fallbackRequest, KUBERNETES_TOKEN_UNAUTHORIZED_ERROR, cacheKey, chain);
        }
        return response;
    }

    private Response proceedWithKeycloakToken(final Request request, final Interceptor.Chain chain) throws IOException {
        final Request fallbackRequest = alterRequest(request, fallbackAuthHeaderSupplier.get(), true);
        log.debug("Sending http request to {} using keycloak token", fallbackRequest.url());
        return chain.proceed(fallbackRequest);
    }

    private Response doRequestFallback(final Request fallbackRequest,
                                       final String reason,
                                       final String cacheKey,
                                       final Interceptor.Chain chain) throws IOException {
        log.debug("Sending http request to {} using keycloak token", fallbackRequest.url());
        final Response fallbackResponse = chain.proceed(fallbackRequest);
        if (fallbackResponse.isSuccessful()) {
            urlCache.store(cacheKey);
            if(Objects.equals(reason, KUBERNETES_TOKEN_ACQUISITION_ERROR)) {
                log.warn("Failed to establish m2m connection to {}\n{}", fallbackRequest.url(), reason);
            } else {
                log.debug("Failed to establish m2m connection to {}\n{}", fallbackRequest.url(), reason);
            }
        }
        return fallbackResponse;
    }

    private Request alterRequest(final Request initialRequest, final String authHeader, final boolean useFallbackUrl) {
        if (StringUtils.isEmpty(authHeader)) {
            throw new IllegalStateException("M2M auth header is empty.");
        }
        HttpUrl targetUrl = initialRequest.url();
        if(useFallbackUrl && fallbackBaseUrl != null) {
             targetUrl = rebaseUrl(initialRequest.url(), fallbackBaseUrl);
        }
        return initialRequest.newBuilder()
                .url(targetUrl)
                .header("Authorization", authHeader)
                .build();
    }

    private static HttpUrl rebaseUrl(final HttpUrl original, final HttpUrl base) {
        return original.newBuilder()
                .scheme(base.scheme())
                .host(base.host())
                .port(base.port())
                .build();
    }
}
