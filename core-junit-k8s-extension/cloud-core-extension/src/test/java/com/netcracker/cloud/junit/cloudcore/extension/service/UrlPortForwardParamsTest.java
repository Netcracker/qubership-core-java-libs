package com.netcracker.cloud.junit.cloudcore.extension.service;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UrlPortForwardParamsTest {

    @Test
    void testServiceUrl() {
        UrlPortForwardParams params = new UrlPortForwardParams("https://service:8080");

        assertEquals("service", params.getName());
        assertNull(params.getNamespace());
        assertEquals("service", params.getHost());
        assertEquals(8080, params.getPort());
    }

    @Test
    void testServiceWithNamespaceUrl() {
        UrlPortForwardParams params = new UrlPortForwardParams("https://service.namespace:8080");

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace", params.getHost());
        assertEquals(8080, params.getPort());
    }

    @Test
    void testServiceWithNamespaceWithoutPortUrl() {
        UrlPortForwardParams params = new UrlPortForwardParams("https://service.namespace");

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace", params.getHost());
        assertEquals(80, params.getPort());
    }

    @Test
    void testServiceWithNamespaceAndClusterUrl() {
        UrlPortForwardParams params = new UrlPortForwardParams("https://service.namespace.scv.cluster:8585");

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace.scv.cluster", params.getHost());
        assertEquals(8585, params.getPort());
    }

    @Test
    void testURI() throws Exception {
        URI uri = new URI("https://service.namespace.scv.cluster:8585");
        UrlPortForwardParams params = new UrlPortForwardParams(uri);

        assertEquals("service", params.getName());
        assertEquals("namespace", params.getNamespace());
        assertEquals("service.namespace.scv.cluster", params.getHost());
        assertEquals(8585, params.getPort());
    }

}
