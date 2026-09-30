package com.netcracker.routes.gateway.plugin;

import com.netcracker.routes.gateway.plugin.scan.ScanControllers;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.FACADE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.INTERNAL;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PRIVATE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteScannerDeclarationsTest {

    private final RouteScanner scanner = new RouteScanner(new String[]{"com.netcracker"}, new SystemStreamLog());

    private RouteScanner.Declarations scan(Class<?>... classes) {
        String[] names = Arrays.stream(classes).map(Class::getName).toArray(String[]::new);
        try (ScanResult scan = new ClassGraph().enableAllInfo().acceptClasses(names).scan()) {
            List<ClassInfo> infos = Arrays.stream(names).map(scan::getClassInfo).toList();
            return scanner.collect(infos);
        }
    }

    private Set<HttpRoute> routes(Class<?>... classes) {
        return scan(classes).routes();
    }

    @Test
    void classLevelForbiddenWithoutRoute() {
        RouteScanner.Declarations declarations = scan(ScanControllers.ForbiddenClassWithoutRoute.class);

        assertEquals(Set.of(new ForbiddenPath("/api/v1/svc/order", Set.of(PUBLIC, PRIVATE, INTERNAL))),
                declarations.forbidden());
        assertEquals(Set.of(new HttpRoute("/api/v1/svc/order/{id}/items", PUBLIC, 0)), declarations.routes());
        assertEquals(List.of(), declarations.errors());
    }

    @Test
    void methodLevelForbiddenUsesGatewayPath() {
        RouteScanner.Declarations declarations = scan(ScanControllers.ForbiddenWithGatewayMapping.class);

        assertEquals(Set.of(new ForbiddenPath("/api/x/{id}/secret", Set.of(PUBLIC))), declarations.forbidden());
        assertEquals(Set.of(new HttpRoute("/secret/{id}", "/api/x/{id}/secret", INTERNAL, 0)), declarations.routes());
    }

    @Test
    void forbiddenWithEmptyValue() {
        RouteScanner.Declarations declarations = scan(ScanControllers.ForbiddenEmpty.class);

        assertEquals(Set.of(), declarations.forbidden());
        assertEquals(List.of("@ForbiddenRoute of " + ScanControllers.ForbiddenEmpty.class.getName()
                + "#empty must list PUBLIC, PRIVATE and/or INTERNAL, found []"), declarations.errors());
    }

    @Test
    void forbiddenWithFacade() {
        RouteScanner.Declarations declarations = scan(ScanControllers.ForbiddenFacade.class);

        assertEquals(Set.of(), declarations.forbidden());
        assertEquals(List.of("@ForbiddenRoute of " + ScanControllers.ForbiddenFacade.class.getName()
                + "#facade must list PUBLIC, PRIVATE and/or INTERNAL, found [FACADE]"), declarations.errors());
    }

    @Test
    void twoRouteAnnotationsOnOneMethod() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0), new HttpRoute("/items", FACADE, 0)),
                routes(ScanControllers.TwoRoutes.class));
    }

    @Test
    void borderGatewayNameInGateways() {
        assertEquals(Set.of(new HttpRoute("/items", PRIVATE, 0)), routes(ScanControllers.BorderGatewayName.class));
    }

    @Test
    void anyOtherGatewayNameIsComposite() {
        assertEquals(Set.of(new HttpRoute("/items", "/api/items", FACADE, 0)),
                routes(ScanControllers.CompositeGatewayName.class));
        assertEquals(Set.of(new HttpRoute("/items", FACADE, 0)), routes(ScanControllers.HostsOnCompositeGateway.class));
        assertEquals(Set.of(new HttpRoute("/items", "/composite/items", FACADE, 0)),
                routes(ScanControllers.CompositeWithGatewayPath.class));
    }

    @Test
    void facadeRouteIgnoresBorderGatewayPath() {
        assertEquals(Set.of(new HttpRoute("/items", "/items", FACADE, 0)),
                routes(ScanControllers.FacadeWithBorderMapping.class));
    }

    @Test
    void facadeRouteUsesFacadeGatewayPath() {
        assertEquals(Set.of(new HttpRoute("/items", "/facade/items", FACADE, 0)),
                routes(ScanControllers.FacadeRouteWithFacadeMapping.class));
    }

    @Test
    void methodLevelListReplacesClassLevelList() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0), new HttpRoute("/items/{id}", INTERNAL, 0)),
                routes(ScanControllers.MethodListReplacesClassList.class));
    }

    @Test
    void gatewaysWithOnlyEmptyStringIsEmpty() {
        assertEquals(Set.of(new HttpRoute("/items", PUBLIC, 0)), routes(ScanControllers.EmptyGatewaysString.class));
    }
}
