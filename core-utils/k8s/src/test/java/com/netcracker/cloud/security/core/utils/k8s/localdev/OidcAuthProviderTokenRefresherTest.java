package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class OidcAuthProviderTokenRefresherTest {

    private static final String INSECURE_IDP_TLS_PROPERTY = "security.local-dev.insecure-idp-tls";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer server;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void usesCachedIdTokenWhenNotExpired() {
        ObjectNode config = MAPPER.createObjectNode();
        config.put("id-token", jwtWithExp(Instant.now().plusSeconds(3600)));
        config.put("idp-issuer-url", server.url("/").toString());
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        String token = OidcAuthProviderTokenRefresher.resolveToken(config, httpClient);
        assertEquals(config.get("id-token").asText(), token);
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void refreshesExpiredIdTokenViaDiscoveryAndRefreshGrant() throws Exception {
        String issuer = server.url("/auth/realms/kubernetes").toString().replaceAll("/$", "");
        String tokenEndpoint = server.url("/auth/realms/kubernetes/protocol/openid-connect/token").toString();

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token_endpoint\":\"" + tokenEndpoint + "\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id_token\":\"fresh-id-token\",\"access_token\":\"fresh-access\",\"refresh_token\":\"new-refresh\"}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("id-token", jwtWithExp(Instant.now().minusSeconds(3600)));
        config.put("idp-issuer-url", issuer);
        config.put("refresh-token", "stored-refresh-token");
        config.put("client-id", "kubernetes");
        config.put("client-secret", "secret-value");

        String token = OidcAuthProviderTokenRefresher.resolveToken(config, httpClient);
        assertEquals("fresh-id-token", token);

        RecordedRequest discovery = server.takeRequest(1, TimeUnit.SECONDS);
        assertTrue(discovery.getPath().endsWith("/.well-known/openid-configuration"));

        RecordedRequest refresh = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("POST", refresh.getMethod());
        String body = refresh.getBody().readUtf8();
        assertTrue(body.contains("grant_type=refresh_token"));
        assertTrue(body.contains("refresh_token=stored-refresh-token"));
        assertTrue(body.contains("client_id=kubernetes"));
        assertTrue(body.contains("client_secret=secret-value"));
    }

    @Test
    void fallsBackToCachedIdTokenWhenRefreshFails() {
        ObjectNode config = MAPPER.createObjectNode();
        config.put("id-token", jwtWithExp(Instant.now().minusSeconds(3600)));
        config.put("idp-issuer-url", "https://idp.example.com/auth/realms/kubernetes");
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        String token = OidcAuthProviderTokenRefresher.resolveToken(config, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build());
        assertEquals(config.get("id-token").asText(), token);
    }

    @Test
    void prefersIdTokenFromRefreshResponse() throws Exception {
        String issuer = server.url("/realms/kubernetes").toString().replaceAll("/$", "");
        String tokenEndpoint = server.url("/realms/kubernetes/token").toString();

        server.enqueue(new MockResponse()
                .setBody("{\"token_endpoint\":\"" + tokenEndpoint + "\"}"));
        server.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"only-access\"}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("idp-issuer-url", issuer);
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertEquals("only-access", OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void returnsNullWhenRefreshFieldsMissingAndNoCachedToken() {
        ObjectNode config = MAPPER.createObjectNode();
        config.put("idp-issuer-url", server.url("/").toString());
        assertNull(OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void discoveryUnauthorizedFailsWithoutCachedToken() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("unauthorized"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("idp-issuer-url", server.url("/issuer").toString().replaceAll("/$", ""));
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertThrows(IllegalStateException.class,
                () -> OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void refreshResponseWithoutTokensFails() throws Exception {
        String issuer = server.url("/realms/kubernetes").toString().replaceAll("/$", "");
        String tokenEndpoint = server.url("/realms/kubernetes/token").toString();
        server.enqueue(new MockResponse().setBody("{\"token_endpoint\":\"" + tokenEndpoint + "\"}"));
        server.enqueue(new MockResponse().setBody("{}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("idp-issuer-url", issuer);
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertThrows(IllegalStateException.class,
                () -> OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void createHttpClientUsesInsecureFactoryWhenLocalDevEnabled() {
        System.setProperty(LocalDevMode.ENABLED_PROPERTY, "true");
        try {
            ObjectNode config = MAPPER.createObjectNode();
            config.put("id-token", jwtWithExp(java.time.Instant.now().plusSeconds(3600)));
            assertNotNull(OidcAuthProviderTokenRefresher.resolveToken(config));
        } finally {
            System.clearProperty(LocalDevMode.ENABLED_PROPERTY);
        }
    }

    @Test
    void createHttpClientUsesDefaultSslWhenInsecureIdpTlsDisabled() {
        System.setProperty(LocalDevMode.ENABLED_PROPERTY, "true");
        System.setProperty(INSECURE_IDP_TLS_PROPERTY, "false");
        try {
            ObjectNode config = MAPPER.createObjectNode();
            config.put("id-token", jwtWithExp(Instant.now().plusSeconds(3600)));
            assertNotNull(OidcAuthProviderTokenRefresher.resolveToken(config));
        } finally {
            System.clearProperty(LocalDevMode.ENABLED_PROPERTY);
            System.clearProperty(INSECURE_IDP_TLS_PROPERTY);
        }
    }

    @Test
    void discoveryWithoutTokenEndpointFails() {
        server.enqueue(new MockResponse().setBody("{\"issuer\":\"https://example\"}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("idp-issuer-url", server.url("/issuer").toString().replaceAll("/$", ""));
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertThrows(IllegalStateException.class,
                () -> OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void treatsJwtWithoutExpAsExpired() throws Exception {
        String issuer = server.url("/realms/kubernetes").toString().replaceAll("/$", "");
        String tokenEndpoint = server.url("/realms/kubernetes/token").toString();
        server.enqueue(new MockResponse().setBody("{\"token_endpoint\":\"" + tokenEndpoint + "\"}"));
        server.enqueue(new MockResponse().setBody("{\"id_token\":\"jwt-without-exp\"}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("id-token", jwtWithoutExp());
        config.put("idp-issuer-url", issuer);
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertEquals("jwt-without-exp", OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    @Test
    void treatsUnparseableJwtAsExpired() throws Exception {
        String issuer = server.url("/realms/kubernetes").toString().replaceAll("/$", "");
        String tokenEndpoint = server.url("/realms/kubernetes/token").toString();
        server.enqueue(new MockResponse().setBody("{\"token_endpoint\":\"" + tokenEndpoint + "\"}"));
        server.enqueue(new MockResponse().setBody("{\"id_token\":\"fresh-after-bad-jwt\"}"));

        ObjectNode config = MAPPER.createObjectNode();
        config.put("id-token", "not-a-jwt");
        config.put("idp-issuer-url", issuer);
        config.put("refresh-token", "refresh");
        config.put("client-id", "kubernetes");

        assertEquals("fresh-after-bad-jwt", OidcAuthProviderTokenRefresher.resolveToken(config, httpClient));
    }

    private static String jwtWithoutExp() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"user\"}".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    private static String jwtWithExp(java.time.Instant exp) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + exp.getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
