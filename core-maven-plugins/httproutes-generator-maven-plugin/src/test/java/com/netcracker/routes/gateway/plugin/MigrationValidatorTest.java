package com.netcracker.routes.gateway.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.netcracker.routes.gateway.plugin.Gateway.INTERNAL;
import static com.netcracker.routes.gateway.plugin.Gateway.PRIVATE;
import static com.netcracker.routes.gateway.plugin.Gateway.PUBLIC;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.forbidden;
import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationValidatorTest {

    static final String RESOURCE = "/api/v1/svc/resource";
    static final String ORDER = "/api/v1/svc/order";
    static final String SAMPLE = "x-probe-0";

    /**
     * @return findings of all stages: scan, legacy table, planner and validator
     */
    static List<Finding> findings(boolean auto, RouteDeclarations declarations) {
        return RouteMigration.run(declarations, auto).findings();
    }

    private static List<Finding> findings(DeclaredRoute... routes) {
        return findings(false, new RouteDeclarations(List.of(routes), List.of()));
    }

    private static List<Finding> errors(List<Finding> findings) {
        return findings.stream().filter(Finding::isError).toList();
    }

    private static List<Finding> ofKind(List<Finding> findings, Finding.Kind kind) {
        return findings.stream().filter(f -> f.kind() == kind).toList();
    }

    private static List<String> targets(List<Finding> findings) {
        return findings.stream().map(Finding::target).toList();
    }

    private static List<String> requests(List<Finding> findings) {
        return findings.stream().map(f -> f.detail("request").orElseThrow()).toList();
    }

    private static final List<String> ALL_GATEWAYS = List.of(PUBLIC.refName(), PRIVATE.refName(), INTERNAL.refName());
    private static final List<String> PUBLIC_AND_PRIVATE = List.of(PUBLIC.refName(), PRIVATE.refName());

    static List<DeclaredRoute> implicit404Routes() {
        return List.of(
                route(RESOURCE, RESOURCE, HttpRoute.Type.PUBLIC, "com.acme.ResourceController"),
                route(RESOURCE + "/{id}/internal-api", RESOURCE + "/{id}/internal-api", HttpRoute.Type.INTERNAL,
                        "com.acme.ResourceController#internalApi"));
    }

    // Differential validation over request paths

    @Test
    void cutExposesControllerSubtree() {
        List<Finding> findings = findings(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "items"));

        List<Finding> exposures = ofKind(findings, Finding.Kind.EXPOSURE);
        assertEquals(ALL_GATEWAYS, targets(exposures));
        assertEquals(List.of(ORDER, ORDER, ORDER), requests(exposures));
        assertEquals(exposures, errors(findings));
    }

    @Test
    void cutExposureCoveredByShorterLegacyRoute() {
        List<Finding> findings = findings(
                route(ORDER, "/order", HttpRoute.Type.PUBLIC, "root"),
                route(ORDER + "/{id}/items", "/order/{id}/items", HttpRoute.Type.PUBLIC, "items"));

        assertEquals(List.of(), findings);
    }

    @Test
    void implicitLegacy404NotDeclared() {
        List<Finding> findings = findings(false, new RouteDeclarations(implicit404Routes(), List.of()));

        List<Finding> exposures = ofKind(findings, Finding.Kind.EXPOSURE);
        assertEquals(PUBLIC_AND_PRIVATE, targets(exposures));
        assertEquals(List.of(RESOURCE + "/" + SAMPLE + "/internal-api", RESOURCE + "/" + SAMPLE + "/internal-api"),
                requests(exposures));
        assertEquals(exposures, errors(findings));
    }

    @Test
    void implicitLegacy404ForPathWithoutVariables() {
        List<Finding> findings = findings(
                route(RESOURCE, RESOURCE, HttpRoute.Type.PUBLIC, "root"),
                route(RESOURCE + "/admin", RESOURCE + "/admin", HttpRoute.Type.INTERNAL, "admin"));

        List<Finding> exposures = ofKind(findings, Finding.Kind.EXPOSURE);
        assertEquals(PUBLIC_AND_PRIVATE, targets(exposures));
        assertEquals(List.of(RESOURCE + "/admin", RESOURCE + "/admin"), requests(exposures));
    }

    @Test
    void narrowerRouteWithNoWiderPrefixAbove() {
        List<Finding> findings = findings(route("/api/v1/svc/admin/{id}", "/api/v1/svc/admin/{id}", HttpRoute.Type.INTERNAL, "admin"));

        List<Finding> exposures = ofKind(findings, Finding.Kind.EXPOSURE);
        assertEquals(List.of(INTERNAL.refName()), targets(exposures));
        assertEquals(List.of("/api/v1/svc/admin"), requests(exposures));
        assertEquals(exposures, errors(findings));
    }

    @Test
    void conflictingBehaviorAfterCut() {
        List<Finding> findings = findings(
                route(RESOURCE, "/resource", HttpRoute.Type.PUBLIC, "root"),
                route(RESOURCE + "/{id}/v2-api", "/v2/{id}/v2-api", HttpRoute.Type.PUBLIC, "v2"));

        List<Finding> conflicts = ofKind(findings, Finding.Kind.CONFLICT);
        assertEquals(List.of(String.join(", ", ALL_GATEWAYS)), targets(conflicts));
        assertEquals(List.of(RESOURCE + "/" + SAMPLE + "/v2-api"), requests(conflicts));
        assertEquals(conflicts, errors(findings));
    }

    @Test
    void literalAndVariableAtSamePosition() {
        List<Finding> findings = findings(
                route("/a/b/{id}/c", "/x/{id}/c", HttpRoute.Type.PUBLIC, "B"),
                route("/a/b/lit", "/y", HttpRoute.Type.PUBLIC, "C"));

        List<Finding> conflicts = ofKind(findings, Finding.Kind.CONFLICT);
        assertEquals(ALL_GATEWAYS, targets(conflicts));
        assertEquals(List.of("/a/b/lit/c", "/a/b/lit/c", "/a/b/lit/c"), requests(conflicts));
        String text = conflicts.get(0).details().toString();
        assertTrue(text.contains("/x/lit/c") && text.contains("/y/c"), text);
    }

    @Test
    void onlyTimeoutsDiffer() {
        List<Finding> findings = findings(
                route("/api/a", "/a", HttpRoute.Type.PUBLIC, 5000, "a"),
                route("/api/a/{id}/x", "/a/{id}/x", HttpRoute.Type.PUBLIC, 60000, "b"));

        assertEquals(List.of(), errors(findings));
        List<Finding> changes = ofKind(findings, Finding.Kind.TIMEOUT_CHANGE);
        assertEquals(ALL_GATEWAYS, targets(changes));
        assertEquals(List.of("/api/a", "/api/a", "/api/a"), requests(changes));
    }

    // Undefined legacy precedence

    @Test
    void tiedAllowedAndForbiddenEntries() {
        List<Finding> findings = findings(
                route("/s/{a}/b", "/s/{a}/b", HttpRoute.Type.PUBLIC, "a"),
                route("/s/b/{a}", "/s/b/{a}", HttpRoute.Type.INTERNAL, "b"));

        List<Finding> undefined = ofKind(findings, Finding.Kind.LEGACY_PRECEDENCE_UNDEFINED);
        assertEquals(PUBLIC_AND_PRIVATE, targets(undefined));
        assertEquals(List.of("/s/b/b", "/s/b/b"), requests(undefined));
        String text = undefined.get(0).details().toString();
        assertTrue(text.contains("(from a)") && text.contains("(from b)"), text);
        assertTrue(undefined.get(0).detail("fix").orElseThrow().contains("change the gateway path"), text);
        assertTrue(findings.stream().noneMatch(f -> f.detail("request").orElse("").equals("/s/b/b")
                && f.kind() != Finding.Kind.LEGACY_PRECEDENCE_UNDEFINED), findings.toString());
    }

    @Test
    void tiedRoutesThatDifferOnlyInTimeout() {
        List<Finding> findings = findings(
                route("/s/{a}/b", "/x/{a}/b", HttpRoute.Type.PUBLIC, 5000, "a"),
                route("/s/b/{a}", "/x/b/{a}", HttpRoute.Type.PUBLIC, 60000, "b"));

        assertEquals(List.of(), ofKind(findings, Finding.Kind.LEGACY_PRECEDENCE_UNDEFINED));
    }

    // Legacy-invalid duplicate routes

    @Test
    void sameGatewayPathDifferentServicePaths() {
        List<Finding> findings = findings(
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, "a"),
                route("/api/x", "/b", HttpRoute.Type.INTERNAL, "b"));

        assertEquals(List.of(Finding.Kind.LEGACY_INVALID), findings.stream().map(Finding::kind).toList());
        assertEquals(INTERNAL.refName(), findings.get(0).target());
    }

    @Test
    void legacyInvalidOnlyWhereBothAreAllowed() {
        List<Finding> findings = findings(
                route("/api/x", "/a", HttpRoute.Type.PUBLIC, "a"),
                route("/api/x", "/b", HttpRoute.Type.INTERNAL, "b"));

        assertEquals(List.of(Finding.Kind.LEGACY_INVALID), findings.stream().map(Finding::kind).toList());
        assertEquals(INTERNAL.refName(), findings.get(0).target());
    }

    @Test
    void noUndefinedPrecedenceOrConflictNextToLegacyInvalid() {
        // /s/{a}/b ties with /s/b/{a} for /s/b/b, and the cut gives /s different rewrites; /s/b only covers its cut
        List<Finding> findings = findings(
                route("/s/{a}/b", "/one/{a}/b", HttpRoute.Type.INTERNAL, "a"),
                route("/s/{a}/b", "/other/{a}/b", HttpRoute.Type.INTERNAL, "b"),
                route("/s/b/{a}", "/two/{a}", HttpRoute.Type.INTERNAL, "c"),
                route("/s/b", "/two", HttpRoute.Type.INTERNAL, "d"));

        assertEquals(List.of(Finding.Kind.LEGACY_INVALID), errors(findings).stream().map(Finding::kind).toList(), findings.toString());
    }

    @Test
    void sameRouteWithDifferentTimeoutsIsOnlyWarned() {
        List<Finding> findings = findings(
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, 5000, "a"),
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, 60000, "b"));

        assertEquals(List.of(), errors(findings));
        assertEquals(List.of(), ofKind(findings, Finding.Kind.LEGACY_INVALID));
        assertEquals(1, ofKind(findings, Finding.Kind.TIMEOUT_MERGE).size());
    }

    // The example of regex-routes-migration.md without the another-service routes 3 and 4

    @Test
    void migrationDocumentExample() {
        List<Finding> findings = findings(false, new RouteDeclarations(LegacyRouteTableTest.migrationDocumentRoutes(), List.of()));

        List<Finding> exposures = ofKind(findings, Finding.Kind.EXPOSURE);
        assertEquals(exposures, errors(findings));
        String resource = "/api/v1/my-service/resource";
        String order = "/api/v1/my-service/order";
        assertEquals(List.of(
                        PUBLIC.refName() + " " + order,
                        PUBLIC.refName() + " " + resource + "/" + SAMPLE + "/internal-api",
                        PRIVATE.refName() + " " + order,
                        PRIVATE.refName() + " " + resource + "/" + SAMPLE + "/internal-api",
                        INTERNAL.refName() + " " + order),
                exposures.stream().map(f -> f.target() + " " + f.detail("request").orElseThrow()).toList());
    }

    @Test
    void migrationDocumentExampleWithForbiddenRoutes() {
        String resource = "/api/v1/my-service/resource";
        String order = "/api/v1/my-service/order";
        List<ForbiddenDeclaration> forbidden = List.of(
                forbidden(resource + "/{var1}/internal-api", "com.acme.ResourceController#internalApi", PUBLIC, PRIVATE),
                forbidden(order, "com.acme.OrderController", PUBLIC, PRIVATE, INTERNAL));

        List<Finding> findings = findings(false, new RouteDeclarations(LegacyRouteTableTest.migrationDocumentRoutes(), forbidden));

        assertEquals(List.of(), findings);
    }

    // DENY rules and automatic DENY rules in validation

    @Test
    void forbiddenControllerRootClosesCutExposure() {
        List<Finding> findings = findings(false, new RouteDeclarations(
                List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "items")),
                List.of(forbidden(ORDER, "OrderController", PUBLIC, PRIVATE, INTERNAL))));

        assertEquals(List.of(), findings);
    }

    @Test
    void automaticRulesCloseImplicitAndCutExposures() {
        List<DeclaredRoute> implicit = new ArrayList<>(implicit404Routes());
        implicit.add(route(RESOURCE + "/{id}/internal-api/status", RESOURCE + "/{id}/internal-api/status",
                HttpRoute.Type.PUBLIC, "status"));

        assertEquals(List.of(), findings(true, new RouteDeclarations(implicit, List.of())));
        assertEquals(List.of(), findings(true, new RouteDeclarations(
                List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "items")), List.of())));
        assertEquals(List.of(), findings(true, new RouteDeclarations(List.of(
                route(ORDER + "/{id}", ORDER + "/{id}", HttpRoute.Type.INTERNAL, "get"),
                route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "items")), List.of())));
    }

    @Test
    void longerRoutesOverlappingForbiddenPathStayRouted() {
        // legacy routes /a/lit/x/y and /a/bb by the longer routes; the DENY rules exempt them in notPaths
        assertEquals(List.of(), ofKind(findings(false, new RouteDeclarations(
                List.of(route("/a/{id}/x/y", "/a/{id}/x/y", HttpRoute.Type.PUBLIC, "y")),
                List.of(forbidden("/a/lit/x", "x", PUBLIC)))), Finding.Kind.LOST_ROUTE));
        assertEquals(List.of(), ofKind(findings(false, new RouteDeclarations(
                List.of(route("/{p}/bb", "/{p}/bb", HttpRoute.Type.PUBLIC, "bb")),
                List.of(forbidden("/a/{x}", "x", PUBLIC)))), Finding.Kind.LOST_ROUTE));
    }

    @Test
    void automaticDenyRuleOnLegacyRoutedPathIsLostRoute() {
        // the cut exposure rule for order is decided on order only, and the catch-all route, shorter than order,
        // routes order/x in legacy, so it is not in notPaths
        String order = "/api/v1/my-service/order";
        List<Finding> findings = findings(true, new RouteDeclarations(List.of(
                route("/{a}/{b}/{c}/{d}/{e}", "/{a}/{b}/{c}/{d}/{e}", HttpRoute.Type.PUBLIC, "any"),
                route(order + "/{id}/items", order + "/{id}/items", HttpRoute.Type.PUBLIC, "items")), List.of()));

        List<Finding> lost = ofKind(findings, Finding.Kind.LOST_ROUTE).stream()
                .filter(f -> f.detail("request").orElseThrow().equals(order + "/" + SAMPLE)).toList();
        assertEquals(ALL_GATEWAYS, targets(lost));
        Finding finding = lost.get(0);
        assertTrue(finding.detail("problem").orElseThrow().contains("automatic DENY rule from auto: cut exposure of " + order),
                finding.toString());
        String fix = finding.detail("fix").orElseThrow();
        assertTrue(fix.contains("no @ForbiddenRoute declaration to narrow"), fix);
        assertFalse(fix.contains("narrow the @ForbiddenRoute"), fix);
    }

    // Partial-segment variables

    private static List<DeclaredRoute> partialSegmentRoutes() {
        return List.of(
                route("/api/files", "/files", HttpRoute.Type.PUBLIC, "com.acme.FileController"),
                route("/api/files/{name}.txt", "/files/{name}.txt", HttpRoute.Type.PRIVATE, "com.acme.FileController#text"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void partialSegmentVariableExposureAsksToChangeTheGatewayPath(boolean auto) {
        List<Finding> findings = findings(auto, new RouteDeclarations(partialSegmentRoutes(), List.of()));

        assertEquals(List.of(Finding.Kind.EXPOSURE), errors(findings).stream().map(Finding::kind).toList());
        Finding exposure = errors(findings).get(0);
        assertEquals(PUBLIC.refName(), exposure.target());
        assertEquals("/api/files/" + SAMPLE + ".txt", exposure.detail("request").orElseThrow());
        List<String> fixes = exposure.details().stream().filter(d -> d.label().equals("fix")).map(Finding.Detail::text).toList();
        assertEquals(1, fixes.size(), fixes.toString());
        assertTrue(fixes.get(0).startsWith("change the gateway path /api/files/{name}.txt of com.acme.FileController#text"),
                fixes.get(0));
        assertTrue(fixes.get(0).contains("whole-segment"), fixes.get(0));
    }

    // Trailing slash of a controller root

    @Test
    void controllerRootWithTrailingSlashPlansCleanly() {
        assertEquals(List.of(), findings(
                route("/api/x/", "/a/", HttpRoute.Type.PUBLIC, "s"),
                route("/api/x/{id}", "/b/{id}", HttpRoute.Type.PUBLIC, "r")));
        assertEquals(List.of(), findings(
                route("/api/x/", "/api/x/", HttpRoute.Type.PUBLIC, 1000, "s"),
                route("/api/x/{id}", "/api/x/{id}", HttpRoute.Type.PUBLIC, 5000, "r")));
    }

    static RouteDeclarations withElements(List<DeclaredRoute> routes, Map<String, List<String>> elements) {
        Map<String, SortedSet<String>> elementGatewayPaths = new java.util.TreeMap<>();
        elements.forEach((path, names) -> elementGatewayPaths.put(path, new TreeSet<>(names)));
        return new RouteDeclarations(routes, List.of(), List.of(), elementGatewayPaths);
    }
}
