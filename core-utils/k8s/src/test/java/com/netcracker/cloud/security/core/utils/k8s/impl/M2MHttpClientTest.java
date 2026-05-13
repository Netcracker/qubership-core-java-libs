package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class M2MHttpClientTest {
    private static final String TEST_ENDPOINT = "/test/endpoint";
    private static final int TEST_CACHE_SIZE = 10;
    private static final long TEST_CACHE_DURATION_SEC = 60;

    private WireMockServer wireMockServer;
    private HttpClient client;

    private Supplier<String> fallbackSupplier;
    private Supplier<String> k8sSupplier;

    private static final String K8S_TOKEN_HEADER = "Bearer k8s-test-token";
    private static final String FALLBACK_TOKEN_HEADER = "Bearer fallback-test-token";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        System.setProperty("security.m2m.kubernetes.enabled", "true");

        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        UrlCache urlCache = new UrlCache(TEST_CACHE_SIZE, TEST_CACHE_DURATION_SEC);
        fallbackSupplier = Mockito.mock(Supplier.class);
        k8sSupplier = Mockito.mock(Supplier.class);

        when(k8sSupplier.get()).thenReturn(K8S_TOKEN_HEADER);
        when(fallbackSupplier.get()).thenReturn(FALLBACK_TOKEN_HEADER);

        M2MAuthenticator authenticator = new M2MAuthenticator(urlCache, fallbackSupplier, k8sSupplier);
        client = new M2MHttpClient(HttpClient.newHttpClient(), authenticator);
    }

    @AfterEach
    void afterEach() {
        wireMockServer.stop();
        System.clearProperty("security.m2m.kubernetes.enabled");
    }

    @Test
    @SneakyThrows
    void kubernetesTokenAuth_Success() {
        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200).withBody("ok")));

        HttpResponse<String> response = client.send(buildRequest(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)));
    }

    @Test
    @SneakyThrows
    void keycloakTokenAuth_UnauthorizedFallback() {
        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(401)));

        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200).withBody("fallback-ok")));

        HttpResponse<String> response = client.send(buildRequest(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("fallback-ok", response.body());

        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));

        // Second call should go straight to fallback (cached)
        HttpResponse<String> response2 = client.send(buildRequest(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response2.statusCode());

        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(2, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));
    }

    @Test
    @SneakyThrows
    void kubernetesTokenAcquisitionError_Fallback() {
        when(k8sSupplier.get()).thenThrow(new IllegalStateException("K8s failed"));

        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200).withBody("fallback-ok")));

        HttpResponse<String> response = client.send(buildRequest(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        verify(0, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));
    }

    @Test
    @SneakyThrows
    void bothTokensEmpty_ThrowsException() {
        when(k8sSupplier.get()).thenReturn("");
        when(fallbackSupplier.get()).thenReturn("");

        var req = buildRequest();
        var respHandler = HttpResponse.BodyHandlers.ofString();
        assertThrows(IllegalStateException.class, () -> client.send(req, respHandler));
    }

    @Test
    @SneakyThrows
    void fallbackUrl_RebasesHostWhenFallbackOccurs() {
        WireMockServer fallbackServer = new WireMockServer(0);
        fallbackServer.start();
        WireMock.configureFor("localhost", fallbackServer.port());

        fallbackServer.stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200).withBody("from-fallback-server")));

        wireMockServer.stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(401)));

        UrlCache urlCache = new UrlCache(TEST_CACHE_SIZE, TEST_CACHE_DURATION_SEC);
        String fallbackBaseUrl = "http://localhost:" + fallbackServer.port();

        M2MAuthenticator authenticator = new M2MAuthenticator(urlCache, fallbackSupplier, k8sSupplier, fallbackBaseUrl);
        HttpClient clientWithFallback = new M2MHttpClient(HttpClient.newHttpClient(), authenticator);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + TEST_ENDPOINT))
                .GET()
                .build();

        HttpResponse<String> response = clientWithFallback.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("from-fallback-server", response.body());

        wireMockServer.verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        fallbackServer.verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));

        fallbackServer.stop();
    }

    @Test
    @SneakyThrows
    void sendAsync_Works() {
        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200).withBody("async-ok")));

        HttpResponse<String> response = client.sendAsync(buildRequest(), HttpResponse.BodyHandlers.ofString()).get();

        assertEquals(200, response.statusCode());
        assertEquals("async-ok", response.body());
    }

    private HttpRequest buildRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + TEST_ENDPOINT))
                .GET()
                .build();
    }
}
