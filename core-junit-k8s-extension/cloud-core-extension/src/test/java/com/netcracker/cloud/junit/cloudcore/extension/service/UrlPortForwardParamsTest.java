package com.netcracker.cloud.junit.cloudcore.extension.service;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UrlPortForwardParamsTest {

    @Test
    void testServiceUrl() {
        UrlPortForwardParams<URL> params = UrlPortForwardParams.builderAsUrl("https://service:8080").build();
        assertEquals("service", params.getName());
        assertNull(params.getNamespace());
        assertEquals("service", params.getHost());
        assertEquals(8080, params.getPort());
    }

    @Test
    void testServiceWithNamespaceUrl() {
        UrlPortForwardParams<URL> params = UrlPortForwardParams.builderAsUrl("https://service.namespace:8080").build();

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace", params.getHost());
        assertEquals(8080, params.getPort());
    }

    @Test
    void testServiceWithNamespaceWithoutPortUrl() {
        UrlPortForwardParams<URL> params = UrlPortForwardParams.builderAsUrl("https://service.namespace").build();

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace", params.getHost());
        assertEquals(443, params.getPort());
    }

    @Test
    void testServiceWithNamespaceAndClusterUrl() {
        UrlPortForwardParams<URL> params = UrlPortForwardParams.builderAsUrl("https://service.namespace.scv.cluster:8585").build();

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace.scv.cluster", params.getHost());
        assertEquals(8585, params.getPort());
    }

}
