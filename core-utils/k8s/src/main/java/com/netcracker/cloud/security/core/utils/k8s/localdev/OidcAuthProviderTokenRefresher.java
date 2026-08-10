package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import java.util.Objects;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.APPLICATION_FORM_URLENCODED;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.CONTENT_TYPE_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_DISCOVERY_TOKEN_ENDPOINT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_FORM_CLIENT_ID;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_FORM_CLIENT_SECRET;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_FORM_GRANT_TYPE;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_GRANT_REFRESH_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_TOKEN_ACCESS_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.OIDC_TOKEN_ID_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.WELL_KNOWN_OPENID_CONFIGURATION_PATH;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.CLIENT_ID;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.CLIENT_SECRET;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.IDP_ISSUER_URL;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.ID_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.ACCESS_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.REFRESH_TOKEN;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OidcAuthProviderTokenRefresher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    static String resolveToken(JsonNode authProviderConfig) {
        return resolveToken(authProviderConfig, createHttpClient());
    }

    // visible for tests
    static String resolveToken(JsonNode authProviderConfig, HttpClient httpClient) {
        Objects.requireNonNull(authProviderConfig, "authProviderConfig");
        Objects.requireNonNull(httpClient, "httpClient");

        String cachedIdToken = LocalDevUtils.firstNonBlank(
                LocalDevUtils.getTextField(authProviderConfig, ID_TOKEN),
                LocalDevUtils.getTextField(authProviderConfig, ACCESS_TOKEN));
        if (StringUtils.isNotBlank(cachedIdToken) && !isExpired(cachedIdToken)) {
            log.debug("Using non-expired OIDC id-token from kubeconfig auth-provider");
            return cachedIdToken;
        }

        String issuerUrl = LocalDevUtils.getTextField(authProviderConfig, IDP_ISSUER_URL);
        String refreshToken = LocalDevUtils.getTextField(authProviderConfig, REFRESH_TOKEN);
        String clientId = LocalDevUtils.getTextField(authProviderConfig, CLIENT_ID);
        String clientSecret = LocalDevUtils.getTextField(authProviderConfig, CLIENT_SECRET);

        if (StringUtils.isAnyBlank(issuerUrl, refreshToken, clientId)) {
            if (StringUtils.isNotBlank(cachedIdToken)) {
                log.warn("OIDC auth-provider id-token is expired/missing refresh fields; "
                        + "falling back to cached token (TokenRequest may fail with 401)");
                return cachedIdToken;
            }
            return null;
        }

        try {
            log.info("Refreshing OIDC kubeconfig token via idp-issuer-url={}", issuerUrl);
            String tokenEndpoint = discoverTokenEndpoint(httpClient, issuerUrl);
            return refreshIdToken(httpClient, tokenEndpoint, clientId, clientSecret, refreshToken);
        } catch (RuntimeException e) {
            if (StringUtils.isNotBlank(cachedIdToken)) {
                log.warn("OIDC token refresh failed; falling back to cached id-token from kubeconfig. "
                        + "If TokenRequest fails with 401, refresh kubeconfig (kubectl login) or import IdP CA into JVM trust store.", e);
                return cachedIdToken;
            }
            throw e;
        }
    }

    private static HttpClient createHttpClient() {
        if (LocalDevMode.isEnabled()) {
            return KubeConfigHttpClientFactory.createInsecureForLocalDev();
        }
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(HTTP_REQUEST_TIMEOUT)
                .build();
    }

    private static String discoverTokenEndpoint(HttpClient httpClient, String issuerUrl) {
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

    private static String refreshIdToken(HttpClient httpClient,
                                         String tokenEndpoint,
                                         String clientId,
                                         String clientSecret,
                                         String refreshToken) {
        try {
            StringBuilder form = new StringBuilder();
            appendForm(form, OIDC_FORM_GRANT_TYPE, OIDC_GRANT_REFRESH_TOKEN);
            appendForm(form, OIDC_GRANT_REFRESH_TOKEN, refreshToken);
            appendForm(form, OIDC_FORM_CLIENT_ID, clientId);
            if (StringUtils.isNotBlank(clientSecret)) {
                appendForm(form, OIDC_FORM_CLIENT_SECRET, clientSecret);
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
            String token = LocalDevUtils.firstNonBlank(
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
    private static boolean isExpired(String jwt) {
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

    private static void appendForm(StringBuilder form, String key, String value) {
        if (!form.isEmpty()) {
            form.append('&');
        }
        form.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
