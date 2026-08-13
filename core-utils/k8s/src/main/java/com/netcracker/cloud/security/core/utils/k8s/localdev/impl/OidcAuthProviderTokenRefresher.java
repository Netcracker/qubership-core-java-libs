package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.APPLICATION_FORM_URLENCODED;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.CONTENT_TYPE_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.KubeConfigFields;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_DISCOVERY_TOKEN_ENDPOINT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_FORM_CLIENT_ID;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_FORM_CLIENT_SECRET;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_FORM_GRANT_TYPE;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_GRANT_REFRESH_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_TOKEN_ACCESS_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.OIDC_TOKEN_ID_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.WELL_KNOWN_OPENID_CONFIGURATION_PATH;

@Slf4j
class OidcAuthProviderTokenRefresher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    String resolveToken(JsonNode authProviderConfig) {
        return resolveToken(authProviderConfig, createHttpClient());
    }

    String resolveToken(JsonNode authProviderConfig, HttpClient httpClient) {
        OidcAuthProviderConfig config = MAPPER.convertValue(authProviderConfig, OidcAuthProviderConfig.class);
        String cachedToken = config.cachedToken();
        if (StringUtils.isNotBlank(cachedToken) && !isExpired(cachedToken)) {
            log.debug("Using non-expired OIDC id-token from kubeconfig auth-provider");
            return cachedToken;
        }

        if (!config.canRefresh()) {
            if (StringUtils.isNotBlank(cachedToken)) {
                log.warn("OIDC auth-provider id-token is expired/missing refresh fields; "
                        + "falling back to cached token (TokenRequest may fail with 401)");
                return cachedToken;
            }
            return null;
        }

        try {
            log.info("Refreshing OIDC kubeconfig token via idp-issuer-url={}", config.idpIssuerUrl());
            String tokenEndpoint = discoverTokenEndpoint(httpClient, config.idpIssuerUrl());
            return refreshIdToken(httpClient, tokenEndpoint, config);
        } catch (RuntimeException e) {
            if (StringUtils.isNotBlank(cachedToken)) {
                log.warn("OIDC token refresh failed; falling back to cached id-token from kubeconfig. "
                        + "If TokenRequest fails with 401, refresh kubeconfig (kubectl login) or import IdP CA into JVM trust store.", e);
                return cachedToken;
            }
            throw e;
        }
    }

    private HttpClient createHttpClient() {
        if (LocalDevMode.isEnabled()) {
            return new KubeConfigHttpClientFactory().createInsecureForLocalDev();
        }
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(HTTP_REQUEST_TIMEOUT)
                .build();
    }

    private String discoverTokenEndpoint(HttpClient httpClient, String issuerUrl) {
        String discoveryUrl = StringUtils.stripEnd(issuerUrl, "/") + WELL_KNOWN_OPENID_CONFIGURATION_PATH;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discoveryUrl))
                    .timeout(HTTP_REQUEST_TIMEOUT)
                    .header(ACCEPT_HEADER, APPLICATION_JSON)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LocalDevUtils.ensureSuccessful(response, "OIDC discovery for " + discoveryUrl);

            String tokenEndpoint = LocalDevUtils.getTextField(
                    MAPPER.readTree(response.body()), OIDC_DISCOVERY_TOKEN_ENDPOINT);
            if (StringUtils.isBlank(tokenEndpoint)) {
                throw new IllegalStateException("OIDC discovery response has no token_endpoint: " + discoveryUrl);
            }
            return tokenEndpoint;
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OIDC discovery interrupted: " + discoveryUrl, e);
        } catch (Exception e) {
            throw new IllegalStateException("OIDC discovery failed for " + discoveryUrl, e);
        }
    }

    private String refreshIdToken(HttpClient httpClient, String tokenEndpoint, OidcAuthProviderConfig config) {
        try {
            StringBuilder form = new StringBuilder();
            appendForm(form, OIDC_FORM_GRANT_TYPE, OIDC_GRANT_REFRESH_TOKEN);
            appendForm(form, OIDC_GRANT_REFRESH_TOKEN, config.refreshToken());
            appendForm(form, OIDC_FORM_CLIENT_ID, config.clientId());
            if (StringUtils.isNotBlank(config.clientSecret())) {
                appendForm(form, OIDC_FORM_CLIENT_SECRET, config.clientSecret());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .timeout(HTTP_REQUEST_TIMEOUT)
                    .header(CONTENT_TYPE_HEADER, APPLICATION_FORM_URLENCODED)
                    .header(ACCEPT_HEADER, APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LocalDevUtils.ensureSuccessful(response, "OIDC refresh_token grant for " + tokenEndpoint);

            JsonNode body = MAPPER.readTree(response.body());
            String token = StringUtils.firstNonBlank(
                    LocalDevUtils.getTextField(body, OIDC_TOKEN_ID_TOKEN),
                    LocalDevUtils.getTextField(body, OIDC_TOKEN_ACCESS_TOKEN));
            if (StringUtils.isBlank(token)) {
                throw new IllegalStateException(
                        "OIDC token response has neither id_token nor access_token: " + tokenEndpoint);
            }
            return token;
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OIDC refresh_token grant interrupted: " + tokenEndpoint, e);
        } catch (Exception e) {
            throw new IllegalStateException("OIDC refresh_token grant failed for " + tokenEndpoint, e);
        }
    }

    /**
     * Checks JWT {@code exp} by decoding the payload segment only (no signature verification).
     * jose4j is on the classpath but a minimal parse avoids full JWT validation for a kubeconfig cache hint.
     */
    private boolean isExpired(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            byte[] payload = Base64.getUrlDecoder().decode(LocalDevUtils.padBase64Url(parts[1]));
            JsonNode claims = MAPPER.readTree(payload);
            JsonNode expNode = claims.path("exp");
            if (!expNode.isNumber()) {
                return true;
            }
            Instant exp = Instant.ofEpochSecond(expNode.asLong());
            return Instant.now().plus(EXPIRY_SKEW).isAfter(exp);
        } catch (Exception e) {
            log.debug("Failed to parse JWT exp from kubeconfig OIDC token, treating as expired: {}", e.toString());
            return true;
        }
    }

    private void appendForm(StringBuilder form, String key, String value) {
        if (!form.isEmpty()) {
            form.append('&');
        }
        form.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OidcAuthProviderConfig(
            @JsonProperty(KubeConfigFields.ID_TOKEN) String idToken,
            @JsonProperty(KubeConfigFields.ACCESS_TOKEN) String accessToken,
            @JsonProperty(KubeConfigFields.REFRESH_TOKEN) String refreshToken,
            @JsonProperty(KubeConfigFields.IDP_ISSUER_URL) String idpIssuerUrl,
            @JsonProperty(KubeConfigFields.CLIENT_ID) String clientId,
            @JsonProperty(KubeConfigFields.CLIENT_SECRET) String clientSecret) {

        String cachedToken() {
            return StringUtils.firstNonBlank(idToken, accessToken);
        }

        boolean canRefresh() {
            return StringUtils.isNoneBlank(idpIssuerUrl, refreshToken, clientId);
        }
    }
}
