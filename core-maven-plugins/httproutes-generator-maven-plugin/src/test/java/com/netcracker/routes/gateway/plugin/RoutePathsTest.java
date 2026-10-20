package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePathsTest {

    @Test
    void cut() {
        assertEquals("/api/v1/svc/order", RoutePaths.cut("/api/v1/svc/order/{id}/items"));
        assertEquals("/api/v1/svc/order", RoutePaths.cut("/api/v1/svc/order"));
        assertEquals("/x", RoutePaths.cut("/x/"));
        assertEquals("/files", RoutePaths.cut("/files/{name}.txt"));
        assertEquals("/files", RoutePaths.cut("/files/report-{id}/x"));
        assertEquals("/", RoutePaths.cut("/{id}"));
        assertEquals("/", RoutePaths.cut("/"));
    }

    @Test
    void overlaps() {
        assertTrue(RoutePaths.overlaps("/a/{id}/x", "/a/lit/x"));
        assertTrue(RoutePaths.overlaps("/a/lit/x", "/a/{id}/x/y"));
        assertTrue(RoutePaths.overlaps("/a", "/a/b/c"));
        assertTrue(RoutePaths.overlaps("/a/{id}", "/a/{name}.txt"));
        assertTrue(RoutePaths.overlaps("/files/{name}.txt", "/files/report.txt"));
        assertFalse(RoutePaths.overlaps("/files/{name}.txt", "/files/report.pdf"));
        assertFalse(RoutePaths.overlaps("/a/lit/x", "/a/{id}/y"));
        assertFalse(RoutePaths.overlaps("/a/b", "/a/c"));
    }

    @Test
    void template() {
        assertEquals("/a/{*}/items", RoutePaths.template("/a/{id}/items/"));
        assertEquals("/", RoutePaths.template("/"));
        assertTrue(RoutePaths.expressible("/a/{id}/items"));
        assertFalse(RoutePaths.expressible("/files/{name}.txt"));
    }

    @Test
    void sample() {
        assertEquals("/a/b/~/c", RoutePaths.sample("/a/{id}", "/a/b/{x}/c"));
        assertEquals("/a/b/~/c", RoutePaths.sample("/a/b/{x}/c", "/a/{id}"));
        assertEquals("/files/~.txt", RoutePaths.sample("/files/{id}", "/files/{name}.txt"));
        assertEquals("/", RoutePaths.sample("/", "/"));
    }
}
