package com.netcracker.cloud.security.core.utils.k8s.impl;

import org.junit.jupiter.api.Test;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class M2MInterceptorTest {
    private static final String TEST_ENDPOINT = "/test/endpoint";
    private static final int TEST_CACHE_SIZE = 10;
    private static final long TEST_CACHE_DURATION_SEC = 60;

    private WireMockServer wireMockServer;
    private OkHttpClient client;

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

        // Default behavior: return valid tokens
        when(k8sSupplier.get()).thenReturn(K8S_TOKEN_HEADER);
        when(fallbackSupplier.get()).thenReturn(FALLBACK_TOKEN_HEADER);

        final M2MInterceptor interceptor = new M2MInterceptor(urlCache, fallbackSupplier, k8sSupplier, true);

        client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
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
                .willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(alterRequest()).execute()) {
            assertEquals(200, response.code());
        }

        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)));
    }

    @Test
    @SneakyThrows
    void keycloakTokenAuth_UnauthorizedFallback() {
        // 1. First call with K8s token returns 401
        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(401)));

        // 2. Fallback call with Keycloak token returns 200
        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(alterRequest()).execute()) {
            assertEquals(200, response.code());
        }

        // Verify both requests were made
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));

        // 3. Second call should go STRAIGHT to fallback because URL is now cached as "non-k8s"
        try (Response response = client.newCall(alterRequest()).execute()) {
            assertEquals(200, response.code());
        }

        // Total count for fallback should be 2, but K8s should still be 1
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(2, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));
    }

    @Test
    @SneakyThrows
    void kubernetesTokenAcquisitionError_Fallback() {
        // Simulate acquisition error
        when(k8sSupplier.get()).thenThrow(new IllegalStateException("K8s failed"));

        stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200)));

        try (Response response = client.newCall(alterRequest()).execute()) {
            assertEquals(200, response.code());
        }

        // Verify it never tried K8s at the network level and went straight to fallback
        verify(0, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT)).withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));
    }

    @Test
    @SneakyThrows
    void bothTokensEmpty_ThrowsException() {
        when(k8sSupplier.get()).thenReturn("");
        when(fallbackSupplier.get()).thenReturn("");

        var req = client.newCall(alterRequest());
        assertThrows(IllegalStateException.class, req::execute);
    }

    private Request alterRequest() {
        return new Request.Builder()
                .url(wireMockServer.baseUrl() + TEST_ENDPOINT)
                .get()
                .build();
    }

    @Test
    @SneakyThrows
    void fallbackUrl_RebasesHostWhenFallbackOccurs() {
        WireMockServer fallbackServer = new WireMockServer(0);
        fallbackServer.start();
        WireMock.configureFor("localhost", fallbackServer.port());

        fallbackServer.stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(200)));

        wireMockServer.stubFor(get(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER))
                .willReturn(aResponse().withStatus(401)));

        UrlCache urlCache = new UrlCache(TEST_CACHE_SIZE, TEST_CACHE_DURATION_SEC);
        String fallbackBaseUrl = "http://localhost:" + fallbackServer.port();

        M2MInterceptor interceptor = new M2MInterceptor(urlCache, fallbackSupplier, k8sSupplier, fallbackBaseUrl, true);
        OkHttpClient clientWithFallbackUrl = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Request request = new Request.Builder()
                .url(wireMockServer.baseUrl() + TEST_ENDPOINT)
                .get()
                .build();

        try (Response response = clientWithFallbackUrl.newCall(request).execute()) {
            assertEquals(200, response.code());
        }

        wireMockServer.verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(K8S_TOKEN_HEADER)));
        fallbackServer.verify(1, getRequestedFor(urlEqualTo(TEST_ENDPOINT))
                .withHeader("Authorization", equalTo(FALLBACK_TOKEN_HEADER)));

        fallbackServer.stop();
    }
}
