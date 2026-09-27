package com.netcracker.routes.gateway.plugin;

import com.netcracker.routes.gateway.plugin.scan.ScanControllers;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.FACADE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.INTERNAL;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PRIVATE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteScannerDeclarationsTest {

    private final RouteScanner scanner = new RouteScanner(new String[]{"com.netcracker"}, new SystemStreamLog());

    static RouteDeclarations scan(RouteScanner scanner, Class<?>... classes) {
        String[] names = Arrays.stream(classes).map(Class::getName).toArray(String[]::new);
        try (ScanResult scan = new ClassGraph().enableAllInfo().acceptClasses(names).scan()) {
            List<ClassInfo> infos = Arrays.stream(names).map(scan::getClassInfo).toList();
            return scanner.declarationsOf(infos);
        }
    }

    private RouteDeclarations scan(Class<?>... classes) {
        return scan(scanner, classes);
    }

    private static Set<HttpRoute> routes(RouteDeclarations declarations) {
        return declarations.routes().stream().map(DeclaredRoute::route).collect(Collectors.toSet());
    }

    private static String origin(Class<?> type, String method) {
        return type.getName() + "#" + method;
    }

    // 2.2

    @Test
    void classLevelForbiddenWithoutRoute() {
        RouteDeclarations declarations = scan(ScanControllers.ForbiddenClassWithoutRoute.class);

        assertEquals(List.of(new ForbiddenDeclaration("/api/v1/svc/order",
                        EnumSet.allOf(Gateway.class), ScanControllers.ForbiddenClassWithoutRoute.class.getName())),
                declarations.forbidden());
        assertEquals(Set.of(new HttpRoute("/api/v1/svc/order/{id}/items", PUBLIC, 0)), routes(declarations));
        assertEquals(List.of(), declarations.findings());
    }

    @Test
    void methodLevelForbiddenUsesGatewayPath() {
        RouteDeclarations declarations = scan(ScanControllers.ForbiddenWithGatewayMapping.class);

        assertEquals(List.of(new ForbiddenDeclaration("/api/x/{id}/secret", EnumSet.of(Gateway.PUBLIC),
                        origin(ScanControllers.ForbiddenWithGatewayMapping.class, "secret"))),
                declarations.forbidden());
        assertEquals(Set.of(new HttpRoute("/secret/{id}", "/api/x/{id}/secret", INTERNAL, 0)), routes(declarations));
    }

    @Test
    void sameRouteFromSeveralElementsKeepsAllOrigins() {
        RouteDeclarations declarations = scan(ScanControllers.SharedChild.class, ScanControllers.SharedBase.class);

        assertEquals(1, declarations.routes().size());
        DeclaredRoute route = declarations.routes().get(0);
        assertEquals(new HttpRoute("/shared", PUBLIC, 0), route.route());
        assertEquals(new TreeSet<>(List.of(ScanControllers.SharedBase.class.getName(), ScanControllers.SharedChild.class.getName())),
                route.origins());
    }

    @Test
    void declaredRoutesAreSortedBySpecificity() {
        RouteDeclarations declarations = scan(ScanControllers.MethodListReplacesClassList.class,
                ScanControllers.ForbiddenClassWithoutRoute.class);

        assertEquals(List.of("/api/v1/svc/order/{id}/items", "/items/{id}", "/items"),
                declarations.routes().stream().map(r -> r.route().gatewayPath()).toList());
    }

    @Test
    void elementGatewayPathsRecordClassesAndMethods() {
        RouteDeclarations declarations = scan(ScanControllers.ForbiddenClassWithoutRoute.class);

        assertEquals(Set.of(ScanControllers.ForbiddenClassWithoutRoute.class.getName()),
                declarations.elementGatewayPaths().get("/api/v1/svc/order"));
        assertEquals(Set.of(origin(ScanControllers.ForbiddenClassWithoutRoute.class, "items")),
                declarations.elementGatewayPaths().get("/api/v1/svc/order/{id}/items"));
    }

    // 2.3

    private void assertInvalidForbidden(Class<?> controller, String method, String problemPart) {
        RouteDeclarations declarations = scan(controller);

        assertEquals(List.of(), declarations.forbidden());
        assertEquals(1, declarations.findings().size(), declarations.findings().toString());
        Finding finding = declarations.findings().get(0);
        assertEquals(Finding.Kind.INVALID_FORBIDDEN_ROUTE, finding.kind());
        assertTrue(finding.isError());
        assertEquals(origin(controller, method), finding.detail("element").orElseThrow());
        assertTrue(finding.detail("problem").orElseThrow().contains(problemPart), finding.toString());
    }

    @Test
    void forbiddenWithEmptyValue() {
        assertInvalidForbidden(ScanControllers.ForbiddenEmpty.class, "empty", "empty value");
    }

    @Test
    void forbiddenWithFacade() {
        assertInvalidForbidden(ScanControllers.ForbiddenFacade.class, "facade",
                "@ForbiddenRoute supports only PUBLIC, PRIVATE and INTERNAL");
    }

    @Test
    void forbiddenWithPartialSegmentVariable() {
        assertInvalidForbidden(ScanControllers.ForbiddenPartialSegment.class, "file", "forbidden paths need whole-segment variables");
    }

    @Test
    void forbiddenWithWildcard() {
        assertInvalidForbidden(ScanControllers.ForbiddenWildcard.class, "files", "* wildcards");
    }

    @Test
    void forbiddenWithRegexConstrainedVariable() {
        assertInvalidForbidden(ScanControllers.ForbiddenRegexVariable.class, "item", "regex-constrained variables");
    }

    // 2.4

    @Test
    void twoRouteAnnotationsOnOneMethod() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0), new HttpRoute("/items", FACADE, 0)),
                routes(scan(ScanControllers.TwoRoutes.class)));
    }

    @Test
    void borderGatewayNameInGateways() {
        assertEquals(Set.of(new HttpRoute("/items", PRIVATE, 0)), routes(scan(ScanControllers.BorderGatewayName.class)));
    }

    @Test
    void anyOtherGatewayNameIsComposite() {
        assertEquals(Set.of(new HttpRoute("/items", "/api/items", FACADE, 0)),
                routes(scan(ScanControllers.CompositeGatewayName.class)));
    }

    @Test
    void facadeRouteIgnoresBorderGatewayPath() {
        assertEquals(Set.of(new HttpRoute("/items", "/items", FACADE, 0)),
                routes(scan(ScanControllers.FacadeWithBorderMapping.class)));
    }

    @Test
    void facadeRouteUsesFacadeGatewayPath() {
        assertEquals(Set.of(new HttpRoute("/items", "/facade/items", FACADE, 0)),
                routes(scan(ScanControllers.FacadeRouteWithFacadeMapping.class)));
    }

    @Test
    void methodLevelListReplacesClassLevelList() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0), new HttpRoute("/items/{id}", INTERNAL, 0)),
                routes(scan(ScanControllers.MethodListReplacesClassList.class)));
    }

    @Test
    void gatewaysWithOnlyEmptyStringIsEmpty() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0)), routes(scan(ScanControllers.EmptyGatewaysString.class)));
    }

    // 2.5

    @Test
    void hostsOnBorderGatewayIsLegacyInvalid() {
        RouteDeclarations declarations = scan(ScanControllers.HostsOnBorderGateway.class);

        assertEquals(1, declarations.findings().size());
        Finding finding = declarations.findings().get(0);
        assertEquals(Finding.Kind.LEGACY_INVALID, finding.kind());
        assertTrue(finding.isError());
        assertEquals(origin(ScanControllers.HostsOnBorderGateway.class, "items"), finding.detail("element").orElseThrow());
        assertTrue(finding.detail("problem").orElseThrow().contains("Only composite gateway can have hosts"));
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0)), routes(declarations));
    }

    @Test
    void hostsOnCompositeGatewayIsValid() {
        RouteDeclarations declarations = scan(ScanControllers.HostsOnCompositeGateway.class);

        assertEquals(List.of(), declarations.findings());
        assertEquals(Set.of(new HttpRoute("/items", FACADE, 0)), routes(declarations));
    }
}
