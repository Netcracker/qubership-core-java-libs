package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.ACCEPT_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.APPLICATION_JSON;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.AUTHORIZATION_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.BEARER_PREFIX;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.CONTENT_TYPE_HEADER;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.HTTP_REQUEST_TIMEOUT;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.K8S_TOKEN_STATUS_EXPIRATION;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.K8S_TOKEN_STATUS_TOKEN;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.TOKEN_REQUEST_API_VERSION;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.TOKEN_REQUEST_EXPIRATION_SECONDS;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.TOKEN_REQUEST_KIND;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.TOKEN_REQUEST_SPEC_AUDIENCES;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.TOKEN_REQUEST_SPEC_EXPIRATION_SECONDS;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.KubeConfigFields.STATUS;

@Slf4j
public class TokenRequestClient {

    /** @see LocalDevConstants#TOKEN_REQUEST_EXPIRATION_SECONDS */
    public static final long DEFAULT_EXPIRATION_SECONDS = LocalDevConstants.TOKEN_REQUEST_EXPIRATION_SECONDS;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String serverUrl;
    private final String userToken;

    public TokenRequestClient(KubeConfigCredentials credentials) {
        this(credentials.getServerUrl(), credentials.getUserToken(), KubeConfigHttpClientFactory.create(credentials));
    }

    @VisibleForTesting
    TokenRequestClient(String serverUrl, String userToken, HttpClient httpClient) {
        this.serverUrl = StringUtils.stripEnd(serverUrl, "/");
        this.userToken = userToken;
        this.httpClient = httpClient;
    }

    public TokenRequestResult requestToken(String namespace, String serviceAccountName, String audience) {
        String url = serverUrl + "/api/v1/namespaces/" + namespace
                + "/serviceaccounts/" + serviceAccountName + "/token";
        try {
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
            handleTokenRequestResponse(response, serviceAccountName, namespace);

            JsonNode root = MAPPER.readTree(response.body());
            String token = root.path(STATUS).path(K8S_TOKEN_STATUS_TOKEN).asText(null);
            if (StringUtils.isBlank(token)) {
                throw new IllegalStateException("TokenRequest response has no status.token");
            }
            Instant expiresAt = parseExpiration(root.path(STATUS).path(K8S_TOKEN_STATUS_EXPIRATION).asText(null));
            return new TokenRequestResult(token, expiresAt);
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Local-dev TokenRequest interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Local-dev TokenRequest failed for SA '"
                    + serviceAccountName + "' in namespace '" + namespace + "'", e);
        }
    }

    private static void handleTokenRequestResponse(HttpResponse<String> response,
                                                   String serviceAccountName,
                                                   String namespace) {
        int status = response.statusCode();
        if (LocalDevHttpUtils.isUnauthorized(status)) {
            throw new IllegalStateException(
                    "Local-dev TokenRequest unauthorized (HTTP " + status + ") for SA '"
                            + serviceAccountName + "' in namespace '" + namespace
                            + "'. Check RBAC: permission to create serviceaccounts/token. Response: "
                            + LocalDevJsonUtils.truncateResponseBody(response.body()));
        }
        if (LocalDevHttpUtils.isFailed(status)) {
            throw new IllegalStateException(
                    "Local-dev TokenRequest failed (HTTP " + status + ") for SA '"
                            + serviceAccountName + "' in namespace '" + namespace
                            + "'. Response: " + LocalDevJsonUtils.truncateResponseBody(response.body()));
        }
    }

    private static String buildRequestBody(String audience) throws Exception {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("apiVersion", TOKEN_REQUEST_API_VERSION);
        root.put("kind", TOKEN_REQUEST_KIND);
        ObjectNode spec = root.putObject("spec");
        ArrayNode audiences = spec.putArray(TOKEN_REQUEST_SPEC_AUDIENCES);
        audiences.add(audience);
        spec.put(TOKEN_REQUEST_SPEC_EXPIRATION_SECONDS, TOKEN_REQUEST_EXPIRATION_SECONDS);
        return MAPPER.writeValueAsString(root);
    }

    private static Instant parseExpiration(String expirationTimestamp) {
        if (StringUtils.isNotBlank(expirationTimestamp)) {
            return Instant.parse(expirationTimestamp);
        }
        return Instant.now().plusSeconds(TOKEN_REQUEST_EXPIRATION_SECONDS);
    }

    public record TokenRequestResult(String token, Instant expiresAt) {
    }
}
