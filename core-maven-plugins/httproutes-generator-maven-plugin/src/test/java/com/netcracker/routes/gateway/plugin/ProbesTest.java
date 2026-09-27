package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SortedSet;

import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbesTest {

    @Test
    void sampleCollidesWithNoSegment() {
        assertEquals("x-probe-0", Probes.sample(List.of("/a/{id}")));
        assertEquals("x-probe-2", Probes.sample(List.of("/a/x-probe-0", "/x-probe-1/{id}")));
    }

    @Test
    void literalOfAnotherPatternReplacesVariable() {
        SortedSet<String> probes = Probes.probes(List.of("/a/b/{id}/c", "/a/b/lit"), "s");

        assertTrue(probes.contains("/a/b/lit/c"), probes.toString());
        assertTrue(probes.contains("/a/b/s/c"), probes.toString());
    }

    @Test
    void literalsReplaceOneVariableAtATime() {
        SortedSet<String> probes = Probes.probes(List.of("/a/{x}/{y}", "/a/l/m", "/a/n/p"), "s");

        assertTrue(probes.containsAll(List.of("/a/l/s", "/a/n/s", "/a/s/m", "/a/s/p", "/a/s/s")), probes.toString());
        assertFalse(probes.contains("/a/l/m/x"), probes.toString());
        assertFalse(probes.contains("/a/n/m"), probes.toString());
    }

    @Test
    void literalsComeOnlyFromCompatiblePrefixes() {
        SortedSet<String> probes = Probes.probes(List.of("/a/{id}", "/b/lit"), "s");

        assertFalse(probes.contains("/a/lit"), probes.toString());
    }

    @Test
    void partialSegmentVariable() {
        SortedSet<String> probes = Probes.probes(List.of("/files/{name}.txt", "/files/readme.txt", "/files/image.png"), "s");

        assertTrue(probes.contains("/files/s.txt"), probes.toString());
        assertTrue(probes.contains("/files/readme.txt"), probes.toString());
        assertFalse(probes.contains("/files/image.png.txt"), probes.toString());
    }

    @Test
    void prefixesWithSlashAndExtraSegment() {
        SortedSet<String> probes = Probes.probes(List.of("/a/{id}/b", "/items/"), "s");

        assertEquals(List.of("/", "/a", "/a/", "/a/s", "/a/s/", "/a/s/b", "/a/s/b/", "/a/s/b/s", "/a/s/s",
                "/items", "/items/", "/items/s", "/s"), List.copyOf(probes));
    }

    @Test
    void probesContainCutPrefixes() {
        String order = "/api/v1/svc/order";
        RouteDeclarations declarations = new RouteDeclarations(
                List.of(route(order + "/{id}/items", "/order/{id}/items", HttpRoute.Type.PUBLIC, "items")), List.of());
        IstioPlan plan = new IstioRoutePlanner(false).plan(declarations, LegacyRouteTable.build(declarations));

        SortedSet<String> probes = Probes.probes(declarations, plan);

        assertTrue(probes.containsAll(List.of(order, order + "/", order + "/" + plan.sample())), probes.toString());
    }
}
