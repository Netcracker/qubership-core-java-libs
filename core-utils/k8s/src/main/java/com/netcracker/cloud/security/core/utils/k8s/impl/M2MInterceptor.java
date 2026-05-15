package com.netcracker.cloud.security.core.utils.k8s.impl;

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
            if you do not intend to use a kubernetes token at this time, please roll back to a previous version of the library.
            otherwise, make sure that a kubernetes token with the `netcracker` audience is properly mounted.
            the previous authentication method will be used as a fallback.""";
    public static final String KUBERNETES_TOKEN_UNAUTHORIZED_ERROR = """
            Unauthorized access (http 401).
            During an m2m interaction attempt using a kubernetes token with the `netcracker` audience, a 401 error was received.
            The possible cause is an outdated version of the security library on the server side.
            The previous authentication method will be used as a fallback.""";

    private final boolean k8sEnabled;
    private final UrlCache urlCache;
    private final Supplier<String> fallbackAuthHeaderSupplier;
    private final Supplier<String> k8sAuthHeaderSupplier;
    private final HttpUrl fallbackBaseUrl;

    public M2MInterceptor(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier) {
        this(urlCache, fallbackAuthHeaderSupplier, k8sAuthHeaderSupplier, null);
    }

    public M2MInterceptor(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier, String fallbackBaseUrl) {
        String k8sEnabledProp = System.getProperty("security.m2m.kubernetes.enabled");
        if (k8sEnabledProp == null) {
            k8sEnabledProp = System.getenv("SECURITY_M2M_KUBERNETES_ENABLED");
        }

        this.k8sEnabled = Boolean.parseBoolean(k8sEnabledProp);
        this.urlCache = urlCache;
        this.fallbackAuthHeaderSupplier = fallbackAuthHeaderSupplier;
        this.k8sAuthHeaderSupplier = k8sAuthHeaderSupplier;
        this.fallbackBaseUrl = (fallbackBaseUrl != null) ? HttpUrl.get(fallbackBaseUrl) : null;
    }

    @NotNull
    @Override
    public Response intercept(final Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final String cacheKey = calculateCacheKey(request.url().toString());
        if (k8sEnabled && !urlCache.containsKey(cacheKey)) {
            //first call (no information) / kubernetes token is applicable
            final Request altered;
            try {
                altered = buildRequest(request, k8sAuthHeaderSupplier.get(), false);
            } catch (IllegalStateException|IllegalArgumentException ex) {
                final Request fallbackRequest = buildRequest(request, fallbackAuthHeaderSupplier.get(), true);
                return doRequestFallback(fallbackRequest, KUBERNETES_TOKEN_ACQUISITION_ERROR, cacheKey, chain);
            }
            final Response response = chain.proceed(altered);
            if (response.code() == HTTP_UNAUTHORIZED) {
                //authentication failed, need to use old approach
                response.close();
                final Request fallbackRequest = buildRequest(request, fallbackAuthHeaderSupplier.get(), true);
                return doRequestFallback(fallbackRequest, KUBERNETES_TOKEN_UNAUTHORIZED_ERROR, cacheKey, chain);
            }
            return response;
        }
        final Request fallbackRequest = buildRequest(request, fallbackAuthHeaderSupplier.get(), true);
        return chain.proceed(fallbackRequest);
    }

    private Response doRequestFallback(final Request fallbackRequest,
                                       final String reason,
                                       final String cacheKey,
                                       final Interceptor.Chain chain) throws IOException {
        final Response fallbackResponse = chain.proceed(fallbackRequest);
        if (fallbackResponse.isSuccessful()) {
            urlCache.store(cacheKey);
            if(k8sEnabled && Objects.equals(reason, KUBERNETES_TOKEN_ACQUISITION_ERROR)) {
                log.warn("Failed to establish m2m connection to {}\n{}", fallbackRequest.url(), reason);
            } else {
                log.debug("Failed to establish m2m connection to {}\n{}", fallbackRequest.url(), reason);
            }
        }
        return fallbackResponse;
    }

    private Request buildRequest(final Request initialRequest, final String authHeader, final boolean useFallbackUrl) {
        if (StringUtils.isEmpty(authHeader)) {
            throw new IllegalStateException("M2M auth header is empty.");
        }
        HttpUrl targetUrl = initialRequest.url();
        if(k8sEnabled && useFallbackUrl && fallbackBaseUrl != null) {
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
