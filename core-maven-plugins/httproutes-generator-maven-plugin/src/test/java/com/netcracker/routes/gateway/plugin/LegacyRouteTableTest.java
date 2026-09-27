package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.netcracker.routes.gateway.plugin.Gateway.INTERNAL;
import static com.netcracker.routes.gateway.plugin.Gateway.PRIVATE;
import static com.netcracker.routes.gateway.plugin.Gateway.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyRouteTableTest {

    private static final String RESOURCE = "/api/v1/my-service/resource";
    private static final String ORDER = "/api/v1/my-service/order";

    static DeclaredRoute route(String gatewayPath, String path, HttpRoute.Type type, long timeout, String origin) {
        return new DeclaredRoute(new HttpRoute(path, gatewayPath, type, timeout), new TreeSet<>(List.of(origin)));
    }

    static DeclaredRoute route(String gatewayPath, String path, HttpRoute.Type type, String origin) {
        return route(gatewayPath, path, type, 0, origin);
    }

    static ForbiddenDeclaration forbidden(String gatewayPath, String origin, Gateway... gateways) {
        return new ForbiddenDeclaration(gatewayPath, EnumSet.copyOf(List.of(gateways)), origin);
    }

    private static LegacyRouteTable table(List<DeclaredRoute> routes, List<ForbiddenDeclaration> forbidden) {
        return LegacyRouteTable.build(new RouteDeclarations(routes, forbidden));
    }

    private static LegacyRouteTable table(DeclaredRoute... routes) {
        return table(List.of(routes), List.of());
    }

    private static void assertRouted(LegacyRouteTable table, Gateway gateway, String request, String upstream, long timeout) {
        LegacyRouteTable.Decision decision = table.decide(gateway, request);
        LegacyRouteTable.Decision.Routed routed = assertInstanceOf(LegacyRouteTable.Decision.Routed.class, decision,
                gateway + " " + request + ": " + decision);
        assertEquals(upstream, routed.upstreamPath(), gateway + " " + request);
        assertEquals(timeout, routed.timeout(), gateway + " " + request);
    }

    private static void assertRouted(LegacyRouteTable table, Gateway gateway, String request, String upstream) {
        assertRouted(table, gateway, request, upstream, 0);
    }

    private static void assertForbidden(LegacyRouteTable table, Gateway gateway, String request) {
        LegacyRouteTable.Decision decision = table.decide(gateway, request);
        assertInstanceOf(LegacyRouteTable.Decision.Forbidden.class, decision, gateway + " " + request + ": " + decision);
    }

    private static void assertUnrouted(LegacyRouteTable table, Gateway gateway, String request) {
        LegacyRouteTable.Decision decision = table.decide(gateway, request);
        assertInstanceOf(LegacyRouteTable.Decision.Unrouted.class, decision, gateway + " " + request + ": " + decision);
    }

    private static LegacyRouteTable.Decision.Undefined assertUndefined(LegacyRouteTable table, Gateway gateway, String request) {
        LegacyRouteTable.Decision decision = table.decide(gateway, request);
        return assertInstanceOf(LegacyRouteTable.Decision.Undefined.class, decision, gateway + " " + request + ": " + decision);
    }

    /**
     * Routes 1, 2, 5, 6 and 7 of the example in regex-routes-migration.md; routes 3 and 4 go to another service.
     */
    static List<DeclaredRoute> migrationDocumentRoutes() {
        return List.of(
                route(RESOURCE, "/resource", HttpRoute.Type.PUBLIC, "com.acme.ResourceController"),
                route(RESOURCE + "/{var1}/internal-api", "/resource/{var1}/internal-api", HttpRoute.Type.INTERNAL,
                        "com.acme.ResourceController#internalApi"),
                route(RESOURCE + "/{var1}", "/resource/{var1}", HttpRoute.Type.PUBLIC, "com.acme.ResourceController#get"),
                route(RESOURCE + "/{var1}/internal-api/status", "/resource/{var1}/internal-api/status", HttpRoute.Type.PUBLIC,
                        "com.acme.ResourceController#status"),
                route(ORDER + "/{var1}/items", "/order/{var1}/items", HttpRoute.Type.PUBLIC, "com.acme.OrderController#items"));
    }

    private static void assertMigrationDocumentRouting(LegacyRouteTable table) {
        for (Gateway gateway : Gateway.values()) {
            assertRouted(table, gateway, RESOURCE, "/resource");
            assertRouted(table, gateway, RESOURCE + "/", "/resource/");
            assertRouted(table, gateway, RESOURCE + "/123", "/resource/123");
            assertRouted(table, gateway, RESOURCE + "/123/other", "/resource/123/other");
            assertRouted(table, gateway, RESOURCE + "/123/internal-api/status", "/resource/123/internal-api/status");
            assertRouted(table, gateway, RESOURCE + "/123/internal-api/status/x", "/resource/123/internal-api/status/x");
            assertRouted(table, gateway, ORDER + "/123/items", "/order/123/items");
            assertRouted(table, gateway, ORDER + "/123/items/5", "/order/123/items/5");
            assertUnrouted(table, gateway, ORDER);
            assertUnrouted(table, gateway, ORDER + "/123");
            assertUnrouted(table, gateway, ORDER + "/123/other");
        }
        for (Gateway gateway : List.of(PUBLIC, PRIVATE)) {
            assertForbidden(table, gateway, RESOURCE + "/123/internal-api");
            assertForbidden(table, gateway, RESOURCE + "/123/internal-api/");
            assertForbidden(table, gateway, RESOURCE + "/123/internal-api/other");
        }
        assertRouted(table, INTERNAL, RESOURCE + "/123/internal-api", "/resource/123/internal-api");
        assertRouted(table, INTERNAL, RESOURCE + "/123/internal-api/other", "/resource/123/internal-api/other");
    }

    @Test
    void migrationDocumentExample() {
        LegacyRouteTable table = table(migrationDocumentRoutes(), List.of());

        assertMigrationDocumentRouting(table);
        assertEquals(List.of(), table.findings());
    }

    @Test
    void migrationDocumentExampleWithForbiddenRoute() {
        LegacyRouteTable table = table(migrationDocumentRoutes(),
                List.of(forbidden(RESOURCE + "/{var1}/internal-api", "com.acme.ResourceController#internalApi", PUBLIC, PRIVATE)));

        assertMigrationDocumentRouting(table);
        assertEquals(List.of(), table.findings());
        LegacyRouteTable.Decision.Forbidden decision = assertInstanceOf(LegacyRouteTable.Decision.Forbidden.class,
                table.decide(PUBLIC, RESOURCE + "/1/internal-api"));
        assertEquals(Set.of(LegacyEntry.Kind.IMPLICIT_FORBIDDEN, LegacyEntry.Kind.EXPLICIT_FORBIDDEN),
                Set.copyOf(decision.entries().stream().map(LegacyEntry::kind).toList()));
    }

    @Test
    void narrowerTypeBelowWiderPrefix() {
        LegacyRouteTable table = table(
                route("/api/v1/svc/resource", "/api/v1/svc/resource", HttpRoute.Type.PUBLIC, "C"),
                route("/api/v1/svc/resource/{id}/internal-api", "/api/v1/svc/resource/{id}/internal-api", HttpRoute.Type.INTERNAL, "C#m"));

        assertForbidden(table, PUBLIC, "/api/v1/svc/resource/1/internal-api");
        assertForbidden(table, PRIVATE, "/api/v1/svc/resource/1/internal-api");
        assertRouted(table, INTERNAL, "/api/v1/svc/resource/1/internal-api", "/api/v1/svc/resource/1/internal-api");
    }

    @Test
    void longerAllowedRouteBelowForbiddenOne() {
        LegacyRouteTable table = table(
                route("/api/v1/svc/resource", "/api/v1/svc/resource", HttpRoute.Type.PUBLIC, "C"),
                route("/api/v1/svc/resource/{id}/internal-api", "/api/v1/svc/resource/{id}/internal-api", HttpRoute.Type.INTERNAL, "C#m"),
                route("/api/v1/svc/resource/{id}/internal-api/status", "/api/v1/svc/resource/{id}/internal-api/status",
                        HttpRoute.Type.PUBLIC, "C#status"));

        for (Gateway gateway : Gateway.values()) {
            assertRouted(table, gateway, "/api/v1/svc/resource/1/internal-api/status", "/api/v1/svc/resource/1/internal-api/status");
        }
    }

    @Test
    void allowedWinsOverImplicitForbiddenWithSamePattern() {
        LegacyRouteTable table = table(
                route("/api/x", "/a", HttpRoute.Type.PUBLIC, "C#public"),
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, "C#internal"));

        assertTrue(table.entries(PUBLIC).stream().allMatch(LegacyEntry::isAllowed));
        assertRouted(table, PUBLIC, "/api/x/1", "/a/1");
    }

    @Test
    void facadeRoutesAreNotInTheModel() {
        LegacyRouteTable table = table(route("/facade/items", "/items", HttpRoute.Type.FACADE, "C#m"));

        for (Gateway gateway : Gateway.values()) {
            assertEquals(List.of(), table.entries(gateway));
            assertUnrouted(table, gateway, "/facade/items");
        }
    }

    @Test
    void variableUpstreamKeepsCapturesByPosition() {
        LegacyRouteTable table = table(route("/api/v1/{tenant}/users/{id}", "/users/{id}/tenants/{tenant}", HttpRoute.Type.PUBLIC, "C#m"));

        // positional: the first gateway variable replaces the first service variable
        assertRouted(table, PUBLIC, "/api/v1/t1/users/u1/x", "/users/t1/tenants/u1/x");
    }

    @Test
    void rootServicePath() {
        LegacyRouteTable table = table(route("/api/svc", "/", HttpRoute.Type.PUBLIC, "C"));

        assertRouted(table, PUBLIC, "/api/svc", "/");
        assertRouted(table, PUBLIC, "/api/svc/", "/");
        assertRouted(table, PUBLIC, "/api/svc/a", "/a");
    }

    // ties (D4)

    @Test
    void tiedRoutesWithDifferentUpstreamPaths() {
        LegacyRouteTable table = table(
                route("/s/{a}/b", "/one/{a}/b", HttpRoute.Type.PUBLIC, "C#one"),
                route("/s/b/{a}", "/two/{a}", HttpRoute.Type.PUBLIC, "C#two"));

        for (Gateway gateway : Gateway.values()) {
            LegacyRouteTable.Decision.Undefined undefined = assertUndefined(table, gateway, "/s/b/b");
            assertEquals(List.of("/s/b/{a}", "/s/{a}/b"), undefined.tied().stream().map(e -> e.pattern().source()).toList());
        }
        assertRouted(table, PUBLIC, "/s/x/b", "/one/x/b");
        assertRouted(table, PUBLIC, "/s/b/x", "/two/x");
    }

    @Test
    void tiedAllowedAndForbiddenEntries() {
        LegacyRouteTable table = table(
                route("/s/{a}/b", "/s/{a}/b", HttpRoute.Type.PUBLIC, "C#one"),
                route("/s/b/{a}", "/s/b/{a}", HttpRoute.Type.INTERNAL, "C#two"));

        assertUndefined(table, PUBLIC, "/s/b/b");
        assertUndefined(table, PRIVATE, "/s/b/b");
        assertRouted(table, INTERNAL, "/s/b/b", "/s/b/b");
    }

    @Test
    void tiedRoutesThatDifferOnlyInTimeout() {
        LegacyRouteTable table = table(
                route("/s/{a}/b", "/x/{a}/b", HttpRoute.Type.PUBLIC, 5000, "C#one"),
                route("/s/b/{a}", "/x/b/{a}", HttpRoute.Type.PUBLIC, 60000, "C#two"));

        assertRouted(table, PUBLIC, "/s/b/b", "/x/b/b", 60000);
        assertRouted(table, PUBLIC, "/s/c/b", "/x/c/b", 5000);
        assertEquals(List.of(), table.findings());
    }

    @Test
    void tiedForbiddenEntries() {
        LegacyRouteTable table = table(
                route("/s/{a}/b", "/s/{a}/b", HttpRoute.Type.INTERNAL, "C#one"),
                route("/s/b/{a}", "/t/{a}", HttpRoute.Type.INTERNAL, "C#two"));

        assertForbidden(table, PUBLIC, "/s/b/b");
        assertUndefined(table, INTERNAL, "/s/b/b");
    }

    // 3.3

    @Test
    void forbiddingAnAllowedRoutesOwnPath() {
        String origin = "com.acme.ItemController#items";
        LegacyRouteTable table = table(List.of(route("/api/items", "/items", HttpRoute.Type.PUBLIC, origin)),
                List.of(forbidden("/api/items", origin, PUBLIC)));

        assertEquals(1, table.findings().size());
        Finding finding = table.findings().get(0);
        assertEquals(Finding.Kind.CONTRADICTORY_DECLARATION, finding.kind());
        assertTrue(finding.isError());
        assertEquals("public-gateway", finding.target());
        assertEquals(origin, finding.detail("element").orElseThrow());
        assertEquals("/api/items", finding.detail("path").orElseThrow());
        assertTrue(finding.detail("problem").orElseThrow().contains("public-gateway"));
        assertTrue(table.entries(PUBLIC).stream().allMatch(LegacyEntry::isAllowed));
    }

    @Test
    void forbiddingPathOfNarrowerRouteIsNotContradictory() {
        String origin = "com.acme.ItemController#items";
        LegacyRouteTable table = table(List.of(route("/api/items", "/items", HttpRoute.Type.INTERNAL, origin)),
                List.of(forbidden("/api/items", origin, PUBLIC, PRIVATE)));

        assertEquals(List.of(), table.findings());
        assertForbidden(table, PUBLIC, "/api/items");
    }

    // 3.4

    @Test
    void sameGatewayPathDifferentServicePaths() {
        LegacyRouteTable table = table(
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, "C#a"),
                route("/api/x", "/b", HttpRoute.Type.INTERNAL, "C#b"));

        assertEquals(1, table.findings().size());
        Finding finding = table.findings().get(0);
        assertEquals(Finding.Kind.LEGACY_INVALID, finding.kind());
        assertEquals("internal-gateway-service", finding.target());
        assertEquals("/api/x", finding.detail("path").orElseThrow());
        List<String> routes = new ArrayList<>();
        finding.details().stream().filter(d -> d.label().equals("route")).forEach(d -> routes.add(d.text()));
        assertEquals(List.of("INTERNAL route /api/x -> /a (from C#a)", "INTERNAL route /api/x -> /b (from C#b)"), routes);
        assertEquals(Set.of(PathPattern.of("/api/x")), table.legacyInvalidPatterns(INTERNAL));
        assertEquals(Set.of(), table.legacyInvalidPatterns(PUBLIC));
    }

    @Test
    void onlyOneOfTheDuplicatesAllowedOnAGateway() {
        LegacyRouteTable table = table(
                route("/api/x", "/a", HttpRoute.Type.PUBLIC, "C#a"),
                route("/api/x", "/b", HttpRoute.Type.INTERNAL, "C#b"));

        assertEquals(List.of("internal-gateway-service"), table.findings().stream().map(Finding::target).toList());
        assertEquals(Finding.Kind.LEGACY_INVALID, table.findings().get(0).kind());
        assertEquals(Set.of(), table.legacyInvalidPatterns(PUBLIC));
        assertEquals(Set.of(), table.legacyInvalidPatterns(PRIVATE));
        assertRouted(table, PUBLIC, "/api/x", "/a");
    }

    @Test
    void sameGatewayPathSameServicePathDifferentTimeouts() {
        LegacyRouteTable table = table(
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, 5000, "C#a"),
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, 60000, "C#b"));

        assertEquals(List.of(), table.findings());
        assertEquals(Set.of(), table.legacyInvalidPatterns(INTERNAL));
        assertRouted(table, INTERNAL, "/api/x/1", "/a/1", 60000);
    }
}
