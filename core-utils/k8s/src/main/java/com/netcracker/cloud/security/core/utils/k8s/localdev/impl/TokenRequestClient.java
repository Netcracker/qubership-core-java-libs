package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.AUTHORIZATION_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.BEARER_PREFIX;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.CONTENT_TYPE_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.K8S_TOKEN_STATUS_EXPIRATION;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.K8S_TOKEN_STATUS_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.KubeConfigFields.STATUS;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.TOKEN_REQUEST_API_VERSION;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.TOKEN_REQUEST_EXPIRATION_SECONDS;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.TOKEN_REQUEST_KIND;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.TOKEN_REQUEST_SPEC_AUDIENCES;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.TOKEN_REQUEST_SPEC_EXPIRATION_SECONDS;

@Slf4j
public class TokenRequestClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String serverUrl;
    private final String userToken;

    public TokenRequestClient(KubeConfigCredentials credentials) {
        this(credentials.getServerUrl(), credentials.getUserToken(), new KubeConfigHttpClientFactory().create(credentials));
    }

    TokenRequestClient(String serverUrl, String userToken, HttpClient httpClient) {
        this.serverUrl = StringUtils.stripEnd(serverUrl, "/");
        this.userToken = userToken;
        this.httpClient = httpClient;
    }

    public ServiceAccountToken requestToken(String namespace, String serviceAccountName, String audience) {
        String url = serverUrl + "/api/v1/namespaces/" + namespace
                + "/serviceaccounts/" + serviceAccountName + "/token";
        try {
            HttpResponse<String> response = sendTokenRequest(url, audience, namespace, serviceAccountName);
            return extractToken(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Local-dev TokenRequest interrupted", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Local-dev TokenRequest failed for SA '"
                    + serviceAccountName + "' in namespace '" + namespace + "'", e);
        }
    }

    private HttpResponse<String> sendTokenRequest(String url,
                                                  String audience,
                                                  String namespace,
                                                  String serviceAccountName) throws IOException, InterruptedException {
        String body = buildRequestBody(audience);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_REQUEST_TIMEOUT)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + userToken)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        log.info("Requesting local-dev SA token: namespace={}, sa={}, audience={}, ttl={}s",
                namespace, serviceAccountName, audience, TOKEN_REQUEST_EXPIRATION_SECONDS);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new IllegalStateException(
                    "Local-dev TokenRequest unauthorized (HTTP " + status + ") for SA '"
                            + serviceAccountName + "' in namespace '" + namespace
                            + "'. Check RBAC for serviceaccounts/token. Response: "
                            + LocalDevUtils.truncateResponseBody(response.body()));
        }
        if (LocalDevUtils.isFailed(status)) {
            throw new IllegalStateException(
                    "Local-dev TokenRequest failed (HTTP " + status + ") for SA '"
                            + serviceAccountName + "' in namespace '" + namespace
                            + "'. Response: " + LocalDevUtils.truncateResponseBody(response.body()));
        }
        return response;
    }

    private ServiceAccountToken extractToken(String responseBody) throws JsonProcessingException {
        JsonNode root = MAPPER.readTree(responseBody);
        String token = root.path(STATUS).path(K8S_TOKEN_STATUS_TOKEN).asText(null);
        if (StringUtils.isBlank(token)) {
            throw new IllegalStateException("TokenRequest response has no status.token");
        }
        Instant expiresAt = parseExpiration(root.path(STATUS).path(K8S_TOKEN_STATUS_EXPIRATION).asText(null));
        return new ServiceAccountToken(token, expiresAt);
    }

    private String buildRequestBody(String audience) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("apiVersion", TOKEN_REQUEST_API_VERSION);
        root.put("kind", TOKEN_REQUEST_KIND);
        ObjectNode spec = root.putObject("spec");
        ArrayNode audiences = spec.putArray(TOKEN_REQUEST_SPEC_AUDIENCES);
        audiences.add(audience);
        spec.put(TOKEN_REQUEST_SPEC_EXPIRATION_SECONDS, TOKEN_REQUEST_EXPIRATION_SECONDS);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build local-dev TokenRequest body", e);
        }
    }

    private Instant parseExpiration(String expirationTimestamp) {
        if (StringUtils.isNotBlank(expirationTimestamp)) {
            return Instant.parse(expirationTimestamp);
        }
        return Instant.now().plusSeconds(TOKEN_REQUEST_EXPIRATION_SECONDS);
    }

    public record ServiceAccountToken(String token, Instant expiresAt) {
    }
}
