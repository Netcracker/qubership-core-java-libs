package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.AUTHORIZATION_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.BEARER_PREFIX;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.DEFAULT_KUBERNETES_ISSUER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.JWKS_PATH;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_DISCOVERY_ISSUER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.WELL_KNOWN_OPENID_CONFIGURATION_PATH;

/**
 * Local-dev helpers for Kubernetes OIDC JWKS URL rewrite and kubeconfig credentials.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocalDevKubernetesOidc {

    /** @see LocalDevConstants#DEFAULT_KUBERNETES_ISSUER */
    public static final String DEFAULT_KUBERNETES_ISSUER = LocalDevConstants.DEFAULT_KUBERNETES_ISSUER;
    /** @see LocalDevConstants#JWKS_PATH */
    public static final String JWKS_PATH = LocalDevConstants.JWKS_PATH;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object LOCK = new Object();

    private static volatile KubeConfigCredentials cachedCredentials;
    private static volatile HttpClient cachedHttpClient;

    public static boolean isKubernetesIssuer(String issuerOrUrl) {
        if (StringUtils.isBlank(issuerOrUrl)) {
            return false;
        }
        String normalized = issuerOrUrl.toLowerCase(Locale.ROOT);
        return normalized.contains("kubernetes.default.svc")
                || normalized.contains("kubernetes.default");
    }

    public static String apiServerUrl() {
        return credentials().getServerUrl();
    }

    public static String userToken() {
        return credentials().getUserToken();
    }

    public static String jwksUrl() {
        return apiServerUrl() + JWKS_PATH;
    }

    /**
     * Kubernetes API OIDC discovery and JWKS are served without authentication.
     */
    public static boolean isPublicOidcEndpoint(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        try {
            String path = URI.create(url).getRawPath();
            if (StringUtils.isBlank(path)) {
                return false;
            }
            return path.endsWith(WELL_KNOWN_OPENID_CONFIGURATION_PATH)
                    || path.endsWith(JWKS_PATH)
                    || path.contains("/openid/v1/jwks");
        } catch (IllegalArgumentException e) {
            return url.contains(WELL_KNOWN_OPENID_CONFIGURATION_PATH) || url.contains(JWKS_PATH);
        }
    }

    /**
     * Resolves the Kubernetes token issuer claim from OIDC discovery when a projected SA token is unavailable.
     */
    public static String resolveIssuerClaimFromDiscovery() {
        String discoveryUrl = apiServerUrl() + WELL_KNOWN_OPENID_CONFIGURATION_PATH;
        try {
            JsonNode discovery = MAPPER.readTree(get(discoveryUrl));
            String issuer = discovery.path(OIDC_DISCOVERY_ISSUER).asText(null);
            if (StringUtils.isNotBlank(issuer)) {
                return issuer;
            }
        } catch (Exception e) {
            log.warn("Failed to resolve Kubernetes issuer from discovery at {} in local-dev, using default {}",
                    discoveryUrl, DEFAULT_KUBERNETES_ISSUER, e);
        }
        return DEFAULT_KUBERNETES_ISSUER;
    }

    private static String get(String url) throws Exception {
        return getWithRetry(url, true);
    }

    private static String getWithRetry(String url, boolean retryOnIo) throws Exception {
        try {
            return sendGet(url);
        } catch (IOException e) {
            if (retryOnIo) {
                log.debug("Retrying Kubernetes OIDC request after I/O failure for {}", url, e);
                resetHttpClient();
                return sendGet(url);
            }
            throw e;
        }
    }

    private static String sendGet(String url) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_REQUEST_TIMEOUT)
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .GET();
        if (!isPublicOidcEndpoint(url)) {
            requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + userToken());
        }
        HttpRequest request = requestBuilder.build();
        // HttpClient is cached and reused (connection pool); not closed per request (see httpClient()).
        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        LocalDevHttpUtils.ensureSuccessful(response, "Local-dev Kubernetes OIDC request for " + url);
        return response.body();
    }

    private static KubeConfigCredentials credentials() {
        KubeConfigCredentials existing = cachedCredentials;
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            if (cachedCredentials == null) {
                cachedCredentials = KubeConfigLoader.load();
                log.info("Local-dev kubeconfig: API server {}", cachedCredentials.getServerUrl());
            }
            return cachedCredentials;
        }
    }

    /**
     * Returns a process-wide cached {@link HttpClient} (TLS from kubeconfig).
     * Not used in try-with-resources: the client is long-lived and shared across OIDC calls.
     */
    private static HttpClient httpClient() {
        HttpClient existing = cachedHttpClient;
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            if (cachedHttpClient == null) {
                cachedHttpClient = KubeConfigHttpClientFactory.create(credentials());
            }
            return cachedHttpClient;
        }
    }

    @VisibleForTesting
    static void resetCache() {
        synchronized (LOCK) {
            cachedCredentials = null;
            cachedHttpClient = null;
        }
    }

    private static void resetHttpClient() {
        synchronized (LOCK) {
            cachedHttpClient = null;
        }
    }
}
