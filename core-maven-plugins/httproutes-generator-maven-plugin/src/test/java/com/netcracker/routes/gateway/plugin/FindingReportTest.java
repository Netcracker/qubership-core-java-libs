package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.netcracker.routes.gateway.plugin.LegacyRouteTableTest.route;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.ORDER;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.RESOURCE;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.SAMPLE;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.findings;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.implicit404Routes;
import static com.netcracker.routes.gateway.plugin.MigrationValidatorTest.withElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingReportTest {

    static final class CapturingLog extends SystemStreamLog {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        @Override
        public void error(CharSequence content) {
            errors.add(content.toString());
        }

        @Override
        public void warn(CharSequence content) {
            warnings.add(content.toString());
        }
    }

    private static String report(List<Finding> findings) {
        return String.join("\n", FindingReport.sorted(findings).stream().map(FindingReport::format).toList());
    }

    private static void assertContains(String text, String part) {
        assertTrue(text.contains(part), "expected <" + part + "> in:\n" + text);
    }

    @Test
    void exposureReportContent() {
        List<Finding> findings = findings(false, new RouteDeclarations(implicit404Routes(), List.of()));
        String report = report(findings);

        assertContains(report, "[ROUTE-MIGRATION] EXPOSURE on public-gateway");
        assertContains(report, "[ROUTE-MIGRATION] EXPOSURE on private-gateway");
        assertContains(report, "request: " + RESOURCE + "/" + SAMPLE + "/internal-api");
        assertContains(report, "FORBIDDEN (implicit: INTERNAL route " + RESOURCE + "/{id}/internal-api (from com.acme.ResourceController#internalApi))");
        assertContains(report, "ROUTED by PathPrefix " + RESOURCE + " (from com.acme.ResourceController, com.acme.ResourceController#internalApi)");
        assertContains(report, "add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE}) to com.acme.ResourceController#internalApi");
        assertContains(report, "<autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>");
    }

    @Test
    void cutExposureSuggestionNamesClassMappedToPrefix() {
        List<DeclaredRoute> routes = List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "com.acme.OrderController#items"));
        String report = report(findings(false, withElements(routes, Map.of(
                ORDER, List.of("com.acme.OrderController"),
                ORDER + "/{id}/items", List.of("com.acme.OrderController#items")))));

        assertContains(report, "add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL}) to class com.acme.OrderController");
    }

    @Test
    void cutExposureSuggestionWithoutClassMappedToPrefix() {
        List<DeclaredRoute> routes = List.of(route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "com.acme.OrderController#items"));
        String report = report(findings(false, withElements(routes, Map.of(
                ORDER + "/{id}/items", List.of("com.acme.OrderController#items")))));

        assertContains(report, "add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL}) to a class or method mapped to " + ORDER);
    }

    @Test
    void cutExposureOfPrefixWithLegacyRouteBelowIt() {
        List<DeclaredRoute> routes = List.of(
                route(ORDER + "/{id}", ORDER + "/{id}", HttpRoute.Type.INTERNAL, "com.acme.OrderController#get"),
                route(ORDER + "/{id}/items", ORDER + "/{id}/items", HttpRoute.Type.PUBLIC, "com.acme.OrderController#items"));
        String report = report(findings(false, withElements(routes, Map.of(ORDER, List.of("com.acme.OrderController")))));

        // legacy routes /order/<sample> on the internal gateway but not /order itself
        assertContains(report, "[ROUTE-MIGRATION] EXPOSURE on internal-gateway-service\n    request: " + ORDER + "\n");
        assertContains(report, "add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL}) to class com.acme.OrderController");
        assertContains(report, "add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE}) to com.acme.OrderController#get");
    }

    @Test
    void multipleProblemsAreAllLoggedAndCounted() {
        List<DeclaredRoute> routes = new ArrayList<>(implicit404Routes());
        routes.add(route("/api/v1/svc/other/{id}/a", "/o/{id}/a", HttpRoute.Type.INTERNAL, "a"));
        routes.add(route("/api/v1/svc/other/{id}/b", "/p/{id}/b", HttpRoute.Type.INTERNAL, "b"));
        List<Finding> findings = findings(false, new RouteDeclarations(routes, List.of()));
        CapturingLog log = new CapturingLog();

        FindingReport.log(log, findings);

        assertEquals(3, log.errors.size(), String.join("\n", log.errors));
        assertTrue(log.errors.get(0).startsWith("[ROUTE-MIGRATION] EXPOSURE on public-gateway"), log.errors.get(0));
        assertTrue(log.errors.get(1).startsWith("[ROUTE-MIGRATION] EXPOSURE on private-gateway"), log.errors.get(1));
        assertTrue(log.errors.get(2).startsWith("[ROUTE-MIGRATION] CONFLICT on internal-gateway-service"), log.errors.get(2));
        assertEquals("Route migration validation failed with 3 errors (1 CONFLICT, 2 EXPOSURE), "
                + "see the [ROUTE-MIGRATION] errors in the build log", FindingReport.summary(findings));
    }

    @Test
    void warningsAreLoggedAsWarnings() {
        List<Finding> findings = findings(false, new RouteDeclarations(List.of(
                route("/api/a", "/a", HttpRoute.Type.PUBLIC, 5000, "a"),
                route("/api/a/{id}/x", "/a/{id}/x", HttpRoute.Type.PUBLIC, 60000, "b")), List.of()));
        CapturingLog log = new CapturingLog();

        FindingReport.log(log, findings);

        assertEquals(List.of(), log.errors);
        assertFalse(log.warnings.isEmpty());
    }

    @Test
    void reportTextForLegacyAndServiceFindings() {
        String legacyInvalid = report(findings(false, new RouteDeclarations(List.of(
                route("/api/x", "/a", HttpRoute.Type.INTERNAL, "a"), route("/api/x", "/b", HttpRoute.Type.INTERNAL, "b")), List.of())));
        assertContains(legacyInvalid, "[ROUTE-MIGRATION] LEGACY_INVALID on internal-gateway-service");
        assertContains(legacyInvalid, "several target paths");

        String undefined = report(findings(false, new RouteDeclarations(List.of(
                route("/s/{a}/b", "/s/{a}/b", HttpRoute.Type.PUBLIC, "a"), route("/s/b/{a}", "/s/b/{a}", HttpRoute.Type.INTERNAL, "b")), List.of())));
        assertContains(undefined, "[ROUTE-MIGRATION] LEGACY_PRECEDENCE_UNDEFINED on public-gateway");
        assertContains(undefined, "change the gateway path of one of these routes");

        String service = report(findings(false, new RouteDeclarations(List.of(
                route("/x", "/a", HttpRoute.Type.FACADE, "composite"), route("/x", "/b", HttpRoute.Type.FACADE, "facade")), List.of())));
        assertContains(service, "[ROUTE-MIGRATION] CONFLICT on service-bound HTTPRoute");
        assertContains(service, "different legacy gateways");
    }
}
