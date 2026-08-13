package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.AUTHORIZATION_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.BEARER_PREFIX;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_DISCOVERY_ISSUER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.WELL_KNOWN_OPENID_CONFIGURATION_PATH;

/**
 * Kubeconfig-backed Kubernetes OIDC helpers for local-dev (JWKS URL rewrite, issuer discovery).
 */
@Slf4j
public final class KubeLocalDevConfig {

    /** Well-known in-cluster Kubernetes issuer, used as fallback when OIDC discovery is unavailable. */
    public static final String DEFAULT_KUBERNETES_ISSUER = "https://kubernetes.default.svc"; // NOSONAR
    public static final String JWKS_PATH = "/openid/v1/jwks";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private KubeConfigCredentials credentials;
    private HttpClient httpClient;

    public KubeLocalDevConfig() {
    }

    KubeLocalDevConfig(KubeConfigCredentials credentials) {
        this.credentials = Objects.requireNonNull(credentials, "credentials");
    }

    public boolean isKubernetesIssuer(String issuerOrUrl) {
        if (StringUtils.isBlank(issuerOrUrl)) {
            return false;
        }
        return issuerOrUrl.contains("kubernetes.default.svc")
                || issuerOrUrl.contains("kubernetes.default");
    }

    public String apiServerUrl() {
        return credentials().getServerUrl();
    }

    public String userToken() {
        return credentials().getUserToken();
    }

    public String jwksUrl() {
        return apiServerUrl() + JWKS_PATH;
    }

    /**
     * Kubernetes API OIDC discovery and JWKS are served without authentication.
     */
    public boolean isPublicOidcEndpoint(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        try {
            String path = URI.create(url).getRawPath();
            if (StringUtils.isBlank(path)) {
                return false;
            }
            return path.endsWith(WELL_KNOWN_OPENID_CONFIGURATION_PATH)
                    || path.contains(JWKS_PATH);
        } catch (IllegalArgumentException e) {
            return url.contains(WELL_KNOWN_OPENID_CONFIGURATION_PATH) || url.contains(JWKS_PATH);
        }
    }

    /**
     * Resolves the Kubernetes token issuer claim from OIDC discovery when a projected SA token is unavailable.
     */
    public String resolveIssuerClaimFromDiscovery() {
        String discoveryUrl = apiServerUrl() + WELL_KNOWN_OPENID_CONFIGURATION_PATH;
        try {
            JsonNode discovery = MAPPER.readTree(get(discoveryUrl));
            String issuer = discovery.path(OIDC_DISCOVERY_ISSUER).asText(null);
            if (StringUtils.isNotBlank(issuer)) {
                return issuer;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Local-dev Kubernetes issuer discovery interrupted: " + discoveryUrl, e);
        } catch (IOException | IllegalStateException e) {
            log.warn("Failed to resolve Kubernetes issuer from discovery at {} in local-dev, using default {}",
                    discoveryUrl, DEFAULT_KUBERNETES_ISSUER, e);
        }
        return DEFAULT_KUBERNETES_ISSUER;
    }

    private String get(String url) throws IOException, InterruptedException {
        try {
            return sendGet(url);
        } catch (IOException e) {
            log.debug("Retrying Kubernetes OIDC request after I/O failure for {}", url, e);
            httpClient = null;
            return sendGet(url);
        }
    }

    private String sendGet(String url) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_REQUEST_TIMEOUT)
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .GET();
        if (!isPublicOidcEndpoint(url)) {
            requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + userToken());
        }
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        LocalDevUtils.ensureSuccessful(response, "Local-dev Kubernetes OIDC request for " + url);
        return response.body();
    }

    private KubeConfigCredentials credentials() {
        if (credentials == null) {
            credentials = new KubeConfigLoader().load();
            log.info("Local-dev kubeconfig: API server {}", credentials.getServerUrl());
        }
        return credentials;
    }

    private HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = new KubeConfigHttpClientFactory().create(credentials());
        }
        return httpClient;
    }
}
