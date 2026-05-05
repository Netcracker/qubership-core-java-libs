package com.netcracker.cloud.security.core.utils.k8s.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache.calculateCacheKey;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

@Slf4j
public final class M2MAuthenticator {

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

    @FunctionalInterface
    public interface HttpSender<T> {
        T send(URI url, String authHeader) throws IOException;
    }

    private final UrlCache urlCache;
    private final Supplier<String> fallbackAuthHeaderSupplier;
    private final Supplier<String> k8sAuthHeaderSupplier;
    private final URI fallbackBaseUri;

    public M2MAuthenticator(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier) {
        this(urlCache, fallbackAuthHeaderSupplier, k8sAuthHeaderSupplier, null);
    }

    public M2MAuthenticator(UrlCache urlCache, Supplier<String> fallbackAuthHeaderSupplier, Supplier<String> k8sAuthHeaderSupplier, String fallbackBaseUrl) {
        this.urlCache = urlCache;
        this.fallbackAuthHeaderSupplier = fallbackAuthHeaderSupplier;
        this.k8sAuthHeaderSupplier = k8sAuthHeaderSupplier;
        this.fallbackBaseUri = (fallbackBaseUrl != null) ? URI.create(fallbackBaseUrl) : null;
    }

    public <T> T execute(
            URI requestUrl,
            HttpSender<T> sender,
            Predicate<T> isUnauthorized,
            Predicate<T> isSuccessful,
            Consumer<T> closeResponse) throws IOException {

        final String cacheKey = calculateCacheKey(requestUrl);

        if (!urlCache.containsKey(cacheKey)) {
            try {
                String k8sHeader = k8sAuthHeaderSupplier.get();
                validateAuthHeader(k8sHeader);
                T response = sender.send(requestUrl, k8sHeader);
                if (!isUnauthorized.test(response)) {
                    return response;
                }
                closeResponse.accept(response);
                return doFallback(requestUrl, sender, isSuccessful, KUBERNETES_TOKEN_UNAUTHORIZED_ERROR, cacheKey);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                return doFallback(requestUrl, sender, isSuccessful, KUBERNETES_TOKEN_ACQUISITION_ERROR, cacheKey);
            }
        }

        URI targetUrl = resolveTargetUrl(requestUrl);
        String authHeader = fallbackAuthHeaderSupplier.get();
        validateAuthHeader(authHeader);
        return sender.send(targetUrl, authHeader);
    }

    private <T> T doFallback(
            URI requestUrl,
            HttpSender<T> sender,
            Predicate<T> isSuccessful,
            String reason,
            String cacheKey) throws IOException {

        URI targetUrl = resolveTargetUrl(requestUrl);
        String authHeader = fallbackAuthHeaderSupplier.get();
        validateAuthHeader(authHeader);
        T response = sender.send(targetUrl, authHeader);
        if (isSuccessful.test(response)) {
            urlCache.store(cacheKey);
            log.warn("Failed to establish m2m connection to {}\n{}", targetUrl, reason);
        }
        return response;
    }

    private URI resolveTargetUrl(URI original) {
        if (fallbackBaseUri == null) {
            return original;
        }
        try {
            return new URI(
                    fallbackBaseUri.getScheme(),
                    null,
                    fallbackBaseUri.getHost(),
                    fallbackBaseUri.getPort(),
                    original.getPath(),
                    original.getQuery(),
                    original.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to rebase URI: " + original, e);
        }
    }

    private static void validateAuthHeader(String authHeader) {
        if (StringUtils.isEmpty(authHeader)) {
            throw new IllegalStateException("M2M auth header is empty.");
        }
    }
}
