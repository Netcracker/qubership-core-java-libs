package com.netcracker.routes.gateway.plugin;

import com.netcracker.routes.gateway.plugin.scan.ScanControllers;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.netcracker.routes.gateway.plugin.Gateway.INTERNAL;
import static com.netcracker.routes.gateway.plugin.Gateway.PRIVATE;
import static com.netcracker.routes.gateway.plugin.Gateway.PUBLIC;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.forbidden;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static com.netcracker.routes.gateway.plugin.PlannedRule.MatchType.EXACT;
import static com.netcracker.routes.gateway.plugin.PlannedRule.MatchType.PATH_PREFIX;
import static com.netcracker.routes.gateway.plugin.PlannedRule.RewriteType.REPLACE_FULL_PATH;
import static com.netcracker.routes.gateway.plugin.PlannedRule.RewriteType.REPLACE_PREFIX_MATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IstioRoutePlannerTest {

    private static final String RESOURCE = "/api/v1/svc/resource";
    private static final String ORDER = "/api/v1/svc/order";

    static IstioPlan plan(boolean auto, List<DeclaredRoute> routes, List<ForbiddenDeclaration> forbidden) {
        RouteDeclarations declarations = new RouteDeclarations(routes, forbidden);
        return new IstioRoutePlanner(auto).plan(declarations, LegacyRouteTable.build(declarations));
    }

    private static IstioPlan plan(DeclaredRoute... routes) {
        return plan(false, List.of(routes), List.of());
    }

    /**
     * Rule shape without its sources: resource type, match, rewrite and timeout.
     */
    private record Shape(Type type, PlannedRule.MatchType match, String value, PlannedRule.RewriteType rewrite,
                         String rewriteValue, long timeout) {
    }

    private static Shape shape(Type type, PlannedRule.MatchType match, String value, PlannedRule.RewriteType rewrite,
                               String rewriteValue, long timeout) {
        return new Shape(type, match, value, rewrite, rewriteValue, timeout);
    }

    private static List<Shape> shapes(IstioPlan plan) {
        return plan.rules().stream()
                .map(r -> shape(r.resourceType(), r.matchType(), r.value(), r.rewriteType(), r.rewriteValue(), r.timeout()))
                .toList();
    }

    private static List<Finding.Kind> kinds(IstioPlan plan) {
        return plan.findings().stream().map(Finding::kind).toList();
    }

    private static DenyRule deny(Gateway gateway, List<String> paths, List<String> notPaths) {
        return new DenyRule(gateway, paths, notPaths, "");
    }

    private static List<DenyRule> withoutOrigins(List<DenyRule> rules) {
        return rules.stream().map(r -> deny(r.gateway(), r.paths(), r.notPaths())).toList();
    }

    // 4.1 cut and rewrite

    @Test
    void cutsGatewayPathAtFirstVariable() {
        assertEquals("/api/v1/my-service/resource", PathPattern.cut("/api/v1/my-service/resource/{var1}/internal-api/status"));
        assertEquals("/api/v1/my-service/order", PathPattern.cut("/api/v1/my-service/order/{id}"));
        assertEquals("/api/v1/files", PathPattern.cut("/api/v1/files/{name}.txt"));
        assertEquals("/api/v1/my-service/resource", PathPattern.cut("/api/v1/my-service/resource"));
        assertEquals("/", PathPattern.cut("/{id}"));
    }

    @Test
    void cutsRewriteTheSameWay() {
        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, RESOURCE, REPLACE_PREFIX_MATCH, "/resource", 0)),
                shapes(plan(route(RESOURCE + "/{var1}/sub", "/resource/{var1}/sub", Type.PUBLIC, "a"))));
        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, RESOURCE, REPLACE_PREFIX_MATCH, "/resource", 0)),
                shapes(plan(route(RESOURCE, "/resource", Type.PUBLIC, "a"))));
        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, "/items", null, null, 0)),
                shapes(plan(route("/items/{id}", "/items/{id}", Type.PUBLIC, "a"))));
        assertEquals(Optional.empty(), IstioRoutePlanner.rewriteOf(new HttpRoute("/items/{id}", "/items/{id}", Type.PUBLIC, 0), "/items"));
    }

    @Test
    void identityRewriteAfterCutHasNoFilter() {
        // the paths differ only after the cut, so the cut rewrite equals the match
        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, "/items", null, null, 0)),
                shapes(plan(route("/items/{id}/a", "/items/{id}/b", Type.PUBLIC, "a"))));
    }

    // 4.2 grouping and merging

    @Test
    void collapsedRoutesWithSameRewrite() {
        IstioPlan plan = plan(
                route(RESOURCE, "/resource", Type.PUBLIC, "a"),
                route(RESOURCE + "/{var1}/sub", "/resource/{var1}/sub", Type.PUBLIC, "b"));

        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, RESOURCE, REPLACE_PREFIX_MATCH, "/resource", 0)), shapes(plan));
        assertEquals(2, plan.rules().get(0).sources().size());
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void sameRewriteDifferentTypesGoesIntoWidestResource() {
        IstioPlan plan = plan(
                route("/api/a", "/a", Type.PUBLIC, "a"),
                route("/api/a/{id}/x", "/a/{id}/x", Type.INTERNAL, "b"));

        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, "/api/a", REPLACE_PREFIX_MATCH, "/a", 0)), shapes(plan));
        assertEquals(List.of(), plan.rules(Type.INTERNAL));
    }

    @Test
    void differentTimeoutsMergeWithLargestAndWarn() {
        IstioPlan plan = plan(
                route("/api/a", "/a", Type.PUBLIC, 5000, "a"),
                route("/api/a/{id}/x", "/a/{id}/x", Type.PUBLIC, 60000, "b"));

        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, "/api/a", REPLACE_PREFIX_MATCH, "/a", 60000)), shapes(plan));
        assertEquals(List.of(Finding.Kind.TIMEOUT_MERGE), kinds(plan));
        Finding warning = plan.findings().get(0);
        assertEquals("public-gateway, private-gateway, internal-gateway-service", warning.target());
        String text = warning.details().toString();
        assertTrue(text.contains("/api/a -> /a (from a) [ReplacePrefixMatch /a, timeout 5s]"), text);
        assertTrue(text.contains("/api/a/{id}/x -> /a/{id}/x (from b)"), text);
        assertTrue(warning.detail("timeout").orElseThrow().contains("1m"), text);
    }

    @Test
    void sameRouteWithDifferentTimeoutsGivesOnlyMergeWarning() {
        List<DeclaredRoute> routes = List.of(
                route("/api/x", "/a", Type.INTERNAL, 5000, "a"),
                route("/api/x", "/a", Type.INTERNAL, 60000, "b"));
        RouteDeclarations declarations = new RouteDeclarations(routes, List.of());
        LegacyRouteTable legacy = LegacyRouteTable.build(declarations);
        IstioPlan plan = new IstioRoutePlanner(false).plan(declarations, legacy);

        assertEquals(List.of(), legacy.findings());
        assertEquals(List.of(Finding.Kind.TIMEOUT_MERGE), kinds(plan));
        assertEquals(List.of(shape(Type.INTERNAL, PATH_PREFIX, "/api/x", REPLACE_PREFIX_MATCH, "/a", 60000)), shapes(plan));
    }

    @Test
    void conflictOfLegacyInvalidDuplicatesNamesTheirGatewayPaths() {
        // RouteMigration drops this conflict, because LEGACY_INVALID is reported for /api/x
        IstioPlan plan = plan(route("/api/x", "/a", Type.INTERNAL, "a"), route("/api/x", "/b", Type.INTERNAL, "b"));

        assertEquals(List.of(new IstioPlan.ConflictedPrefix("/api/x", Type.INTERNAL)), plan.conflicted());
        assertEquals(List.of(Finding.Kind.CONFLICT), kinds(plan));
        assertEquals(java.util.Set.of("/api/x"), plan.findings().get(0).gatewayPaths());
    }

    @Test
    void differentRewritesOnOnePrefixConflict() {
        IstioPlan plan = plan(
                route(RESOURCE + "/{var1}/v1-api", "/resource/{var1}/v1-api", Type.PUBLIC, "a"),
                route(RESOURCE + "/{var1}/v2-api", "/v2/resource/{var1}/v2-api", Type.PUBLIC, "b"));

        assertEquals(List.of(), plan.rules());
        assertEquals(List.of(new IstioPlan.ConflictedPrefix(RESOURCE, Type.PUBLIC)), plan.conflicted());
        assertEquals(List.of(Finding.Kind.CONFLICT), kinds(plan));
        Finding conflict = plan.findings().get(0);
        assertEquals("public-gateway, private-gateway, internal-gateway-service", conflict.target());
        assertEquals(RESOURCE + "/" + plan.sample() + "/v1-api", conflict.detail("request").orElseThrow());
        String text = conflict.details().toString();
        assertTrue(text.contains("(from a)") && text.contains("(from b)"), text);
    }

    @Test
    void conflictTargetsOnlyGatewaysWhereBothRoutesAreAllowed() {
        IstioPlan plan = plan(
                route("/api/a/{id}/x", "/a/{id}/x", Type.PUBLIC, "a"),
                route("/api/a/{id}/y", "/b/{id}/y", Type.INTERNAL, "b"));

        assertEquals(List.of(new IstioPlan.ConflictedPrefix("/api/a", Type.PUBLIC)), plan.conflicted());
        assertEquals("internal-gateway-service", plan.findings().get(0).target());
    }

    // 4.3 Exact split

    @Test
    void exactSplitForControllerRootWithDifferentRewrite() {
        IstioPlan plan = plan(
                route("/api/v1/svc/items", "/v1/items", Type.PUBLIC, "s"),
                route("/api/v1/svc/items/{id}", "/v2/items/{id}", Type.PUBLIC, "r"));

        assertEquals(List.of(
                shape(Type.PUBLIC, EXACT, "/api/v1/svc/items", REPLACE_FULL_PATH, "/v1/items", 0),
                shape(Type.PUBLIC, EXACT, "/api/v1/svc/items/", REPLACE_FULL_PATH, "/v1/items/", 0),
                shape(Type.PUBLIC, PATH_PREFIX, "/api/v1/svc/items", REPLACE_PREFIX_MATCH, "/v2/items", 0)), shapes(plan));
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void exactSplitForControllerRootWithTrailingSlash() {
        // Spring: @RequestMapping("/api/x") on the class, @GetMapping("/") and @GetMapping("/{id}") on methods
        IstioPlan plan = plan(
                route("/api/x/", "/a/", Type.PUBLIC, "s"),
                route("/api/x/{id}", "/b/{id}", Type.PUBLIC, "r"));

        assertEquals(List.of(
                shape(Type.PUBLIC, EXACT, "/api/x", REPLACE_FULL_PATH, "/a", 0),
                shape(Type.PUBLIC, EXACT, "/api/x/", REPLACE_FULL_PATH, "/a/", 0),
                shape(Type.PUBLIC, PATH_PREFIX, "/api/x", REPLACE_PREFIX_MATCH, "/b", 0)), shapes(plan));
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void controllerRootWithTrailingSlashSharesRuleOfItsSubtree() {
        IstioPlan plan = plan(
                route("/api/x/", "/x/", Type.PUBLIC, "s"),
                route("/api/x/{id}", "/x/{id}", Type.PUBLIC, "r"));

        assertEquals(List.of(shape(Type.PUBLIC, PATH_PREFIX, "/api/x", REPLACE_PREFIX_MATCH, "/x", 0)), shapes(plan));
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void controllerRootWithTrailingSlashKeepsItsTimeout() {
        IstioPlan plan = plan(
                route("/api/x/", "/api/x/", Type.PUBLIC, 1000, "s"),
                route("/api/x/{id}", "/api/x/{id}", Type.PUBLIC, 5000, "r"));

        assertEquals(List.of(
                shape(Type.PUBLIC, EXACT, "/api/x", null, null, 1000),
                shape(Type.PUBLIC, EXACT, "/api/x/", null, null, 1000),
                shape(Type.PUBLIC, PATH_PREFIX, "/api/x", null, null, 5000)), shapes(plan));
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void exactSplitWithoutRewriteKeepsTimeoutOfRoot() {
        IstioPlan plan = plan(
                route("/items", "/items", Type.INTERNAL, 5000, "s"),
                route("/items/{id}", "/items/{id}", Type.PUBLIC, 60000, "r"));

        assertEquals(List.of(
                shape(Type.INTERNAL, EXACT, "/items", null, null, 5000),
                shape(Type.INTERNAL, EXACT, "/items/", null, null, 5000),
                shape(Type.PUBLIC, PATH_PREFIX, "/items", null, null, 60000)), shapes(plan));
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void exactSplitOnRootHasOneRule() {
        IstioPlan plan = plan(route("/", "/v1", Type.PUBLIC, "s"), route("/{id}", "/v2/{id}", Type.PUBLIC, "r"));

        assertEquals(List.of(
                shape(Type.PUBLIC, EXACT, "/", REPLACE_FULL_PATH, "/v1", 0),
                shape(Type.PUBLIC, PATH_PREFIX, "/", REPLACE_PREFIX_MATCH, "/v2", 0)), shapes(plan));
    }

    @Test
    void noExactSplitWhenRouteContinuesAfterVariable() {
        IstioPlan plan = plan(
                route("/api/v1/svc/items", "/v1/items", Type.PUBLIC, "s"),
                route("/api/v1/svc/items/{id}/details", "/v2/items/{id}/details", Type.PUBLIC, "r"));

        assertEquals(List.of(), plan.rules());
        assertEquals(List.of(Finding.Kind.CONFLICT), kinds(plan));
        assertEquals("PathPrefix /api/v1/svc/items", plan.findings().get(0).detail("match").orElseThrow());
    }

    // 4.4 DENY rule content

    @Test
    void forbiddenRouteWithLongerAllowedRouteBelow() {
        IstioPlan plan = plan(false, List.of(
                        route(RESOURCE, RESOURCE, Type.PUBLIC, "root"),
                        route(RESOURCE + "/{var1}/internal-api", RESOURCE + "/{var1}/internal-api", Type.INTERNAL, "internalApi"),
                        route(RESOURCE + "/{var1}/internal-api/status", RESOURCE + "/{var1}/internal-api/status", Type.PUBLIC, "status")),
                List.of(forbidden(RESOURCE + "/{var1}/internal-api", "internalApi", PUBLIC, PRIVATE)));

        List<String> paths = List.of(RESOURCE + "/{*}/internal-api", RESOURCE + "/{*}/internal-api/{**}");
        List<String> notPaths = List.of(RESOURCE + "/{*}/internal-api/status", RESOURCE + "/{*}/internal-api/status/{**}");
        assertEquals(List.of(deny(PUBLIC, paths, notPaths), deny(PRIVATE, paths, notPaths)), withoutOrigins(plan.denyRules()));
        assertEquals("internalApi", plan.denyRules().get(0).origin());
    }

    @Test
    void forbiddenControllerRootClosesCutExposure() {
        IstioPlan plan = plan(false, List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", Type.PUBLIC, "items")),
                List.of(forbidden(ORDER, "OrderController", PUBLIC, PRIVATE, INTERNAL)));

        List<String> paths = List.of(ORDER, ORDER + "/{**}");
        List<String> notPaths = List.of(ORDER + "/{*}/items", ORDER + "/{*}/items/{**}");
        assertEquals(List.of(deny(PUBLIC, paths, notPaths), deny(PRIVATE, paths, notPaths), deny(INTERNAL, paths, notPaths)),
                withoutOrigins(plan.denyRules()));
    }

    @Test
    void longerAllowedRouteWithVariableOverLiteralOfForbiddenPathIsInNotPaths() {
        IstioPlan plan = plan(false, List.of(route("/a/{id}/x/y", "/a/{id}/x/y", Type.PUBLIC, "y")),
                List.of(forbidden("/a/lit/x", "x", PUBLIC)));

        assertEquals(List.of(deny(PUBLIC, List.of("/a/lit/x", "/a/lit/x/{**}"), List.of("/a/{*}/x/y", "/a/{*}/x/y/{**}"))),
                withoutOrigins(plan.denyRules()));
    }

    @Test
    void longerAllowedRouteThatOverlapsForbiddenPathFromAboveIsInNotPaths() {
        IstioPlan plan = plan(false, List.of(route("/{p}/bb", "/{p}/bb", Type.PUBLIC, "bb")),
                List.of(forbidden("/a/{x}", "x", PUBLIC)));

        assertEquals(List.of(deny(PUBLIC, List.of("/a/{*}", "/a/{*}/{**}"), List.of("/{*}/bb", "/{*}/bb/{**}"))),
                withoutOrigins(plan.denyRules()));
    }

    @Test
    void forbiddenPathWithTrailingSlash() {
        IstioPlan plan = plan(false, List.of(route("/a", "/a", Type.PUBLIC, "a")), List.of(forbidden("/a/b/", "b", PUBLIC)));

        assertEquals(List.of(deny(PUBLIC, List.of("/a/b", "/a/b/{**}"), List.of())), withoutOrigins(plan.denyRules()));
    }

    @Test
    void nestedForbiddenPathsGiveSeparateRules() {
        IstioPlan plan = plan(false, List.of(route("/a/b", "/a/b", Type.PUBLIC, "b")),
                List.of(forbidden("/a/b/{id}/c", "c", PUBLIC), forbidden("/a", "a", PUBLIC)));

        assertEquals(List.of(
                        deny(PUBLIC, List.of("/a", "/a/{**}"), List.of("/a/b", "/a/b/{**}")),
                        deny(PUBLIC, List.of("/a/b/{*}/c", "/a/b/{*}/c/{**}"), List.of())),
                withoutOrigins(plan.denyRules()));
    }

    @Test
    void forbiddenRootPath() {
        IstioPlan plan = plan(false, List.of(route("/a", "/a", Type.PUBLIC, "a")), List.of(forbidden("/", "root", PUBLIC)));

        assertEquals(List.of(deny(PUBLIC, List.of("/", "/{**}"), List.of("/a", "/a/{**}"))), withoutOrigins(plan.denyRules()));
    }

    // 4.5 automatic DENY rules

    private static List<DeclaredRoute> implicitForbiddenRoutes() {
        return List.of(
                route(RESOURCE, RESOURCE, Type.PUBLIC, "root"),
                route(RESOURCE + "/{id}/internal-api", RESOURCE + "/{id}/internal-api", Type.INTERNAL, "internalApi"),
                route(RESOURCE + "/{id}/internal-api/status", RESOURCE + "/{id}/internal-api/status", Type.PUBLIC, "status"));
    }

    @Test
    void noAutomaticRulesWhenParameterIsOff() {
        IstioPlan plan = plan(false, List.of(
                route(RESOURCE, RESOURCE, Type.PUBLIC, "root"),
                route(RESOURCE + "/{id}/internal-api", RESOURCE + "/{id}/internal-api", Type.INTERNAL, "internalApi")), List.of());

        assertEquals(List.of(), plan.denyRules());
    }

    @Test
    void automaticRuleForImplicitForbiddenRoute() {
        IstioPlan plan = plan(true, implicitForbiddenRoutes(), List.of());

        List<String> paths = List.of(RESOURCE + "/{*}/internal-api", RESOURCE + "/{*}/internal-api/{**}");
        List<String> notPaths = List.of(RESOURCE + "/{*}/internal-api/status", RESOURCE + "/{*}/internal-api/status/{**}");
        assertEquals(List.of(deny(PUBLIC, paths, notPaths), deny(PRIVATE, paths, notPaths)), withoutOrigins(plan.denyRules()));
        assertTrue(plan.denyRules().stream().allMatch(DenyRule::isAutomatic));
        assertTrue(plan.denyRules().get(0).origin().contains("implicit forbidden of INTERNAL route"), plan.denyRules().get(0).origin());
    }

    @Test
    void automaticRuleForCutExposure() {
        IstioPlan plan = plan(true, List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", Type.PUBLIC, "items")), List.of());

        List<String> paths = List.of(ORDER, ORDER + "/{**}");
        List<String> notPaths = List.of(ORDER + "/{*}/items", ORDER + "/{*}/items/{**}");
        assertEquals(List.of(deny(PUBLIC, paths, notPaths), deny(PRIVATE, paths, notPaths), deny(INTERNAL, paths, notPaths)),
                withoutOrigins(plan.denyRules()));
        assertEquals("auto: cut exposure of " + ORDER + " from items", plan.denyRules().get(0).origin());
    }

    @Test
    void automaticRuleForCutPrefixWithLegacyRouteBelowIt() {
        IstioPlan plan = plan(true, List.of(
                route(ORDER + "/{id}", ORDER + "/{id}", Type.INTERNAL, "get"),
                route(ORDER + "/{id}/items", ORDER + "/{id}/items", Type.PUBLIC, "items")), List.of());

        List<DenyRule> internal = withoutOrigins(plan.denyRules(INTERNAL));
        assertEquals(List.of(deny(INTERNAL, List.of(ORDER, ORDER + "/{**}"),
                List.of(ORDER + "/{*}", ORDER + "/{*}/items", ORDER + "/{*}/items/{**}", ORDER + "/{*}/{**}"))), internal);
    }

    @Test
    void noAutomaticRuleWhenLegacyExposedTheSubtree() {
        IstioPlan plan = plan(true, List.of(
                route(ORDER, "/order", Type.PUBLIC, "root"),
                route(ORDER + "/{id}/items", "/order/{id}/items", Type.PUBLIC, "items")), List.of());

        assertEquals(List.of(), plan.denyRules());
    }

    @Test
    void noAutomaticRuleWhenIstioDoesNotRouteForbiddenPath() {
        IstioPlan plan = plan(true, List.of(route("/api/v1/svc/admin", "/admin", Type.INTERNAL, "admin")), List.of());

        assertEquals(List.of(), plan.denyRules());
    }

    @Test
    void noAutomaticRuleForPartialSegmentVariable() {
        // neither @ForbiddenRoute nor an AuthorizationPolicy path can express {name}.txt; the validator reports the exposure
        IstioPlan plan = plan(true, List.of(
                route("/api/files", "/files", Type.PUBLIC, "list"),
                route("/api/files/{name}.txt", "/files/{name}.txt", Type.PRIVATE, "text")), List.of());

        assertEquals(List.of(), plan.denyRules());
    }

    @Test
    void explicitAndAutomaticIdenticalRulesAreRenderedOnce() {
        IstioPlan plan = plan(true, implicitForbiddenRoutes(),
                List.of(forbidden(RESOURCE + "/{id}/internal-api", "internalApi", PUBLIC, PRIVATE)));

        assertEquals(2, plan.denyRules().size());
        assertTrue(plan.denyRules().stream().noneMatch(DenyRule::isAutomatic));
    }

    // 4.6 SERVICE target

    @Test
    void noServiceRulesWithoutRewrite() {
        IstioPlan plan = plan(route("/items", "/items", Type.FACADE, "a"), route("/orders", "/orders", Type.FACADE, "b"));

        assertEquals(List.of(), plan.rules());
        assertEquals(List.of(), plan.findings());
    }

    @Test
    void unappliedFacadeTimeoutIsWarned() {
        IstioPlan plan = plan(route("/items", "/items", Type.FACADE, 5000, "a"));

        assertEquals(List.of(), plan.rules());
        assertEquals(List.of(Finding.Kind.TIMEOUT_NOT_APPLIED), kinds(plan));
        assertEquals(Finding.SERVICE_TARGET, plan.findings().get(0).target());
        assertFalse(plan.findings().get(0).isError());
    }

    @Test
    void allServiceRulesWhenOneHasRewrite() {
        IstioPlan plan = plan(route("/facade/items", "/items", Type.FACADE, "a"), route("/orders", "/orders", Type.FACADE, "b"));

        assertEquals(List.of(
                shape(Type.FACADE, PATH_PREFIX, "/facade/items", REPLACE_PREFIX_MATCH, "/items", 0),
                shape(Type.FACADE, PATH_PREFIX, "/orders", null, null, 0)), shapes(plan));
        assertEquals(List.of(), plan.denyRules());
    }

    @Test
    void compositeRouteGoesToServiceTarget() {
        RouteScanner scanner = new RouteScanner(new String[]{"com.netcracker"}, new SystemStreamLog());
        RouteDeclarations declarations = RouteScannerDeclarationsTest.scan(scanner, ScanControllers.CompositeWithGatewayPath.class);
        IstioPlan plan = new IstioRoutePlanner(true).plan(declarations, LegacyRouteTable.build(declarations));

        assertEquals(List.of(shape(Type.FACADE, PATH_PREFIX, "/composite/items", REPLACE_PREFIX_MATCH, "/items", 0)), shapes(plan));
        assertEquals(List.of(), plan.denyRules());
        assertFalse(plan.toString().contains("composite-gw"), plan.toString());
        assertFalse(plan.toString().contains("example.com"), plan.toString());
    }

    @Test
    void facadeAndCompositeRoutesCollide() {
        IstioPlan plan = plan(route("/x", "/a", Type.FACADE, "composite"), route("/x", "/b", Type.FACADE, "facade"));

        assertEquals(List.of(Finding.Kind.CONFLICT), kinds(plan));
        Finding conflict = plan.findings().get(0);
        assertEquals(Finding.SERVICE_TARGET, conflict.target());
        String text = conflict.details().toString();
        assertTrue(text.contains("(from composite)") && text.contains("(from facade)"), text);
        assertTrue(conflict.detail("problem").orElseThrow().contains("different legacy gateways"), text);
        assertEquals(List.of(), plan.rules());
    }

    @Test
    void facadeRoutesNeverGetLegacyEntriesOrDenyRules() {
        List<DeclaredRoute> routes = List.of(route("/facade/items", "/items", Type.FACADE, "a"));
        RouteDeclarations declarations = new RouteDeclarations(routes, List.of());
        LegacyRouteTable legacy = LegacyRouteTable.build(declarations);

        for (Gateway gateway : Gateway.values()) {
            assertEquals(List.of(), legacy.entries(gateway));
        }
        assertEquals(List.of(), new IstioRoutePlanner(true).plan(declarations, legacy).denyRules());
    }
}
