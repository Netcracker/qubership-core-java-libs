package com.netcracker.routes.gateway.plugin;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.INTERNAL;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PRIVATE;
import static com.netcracker.routes.gateway.plugin.HttpRoute.Type.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Scans the controller sets in the {@code inheritance} test packages: Spring controllers inherit every annotation like
 * in route-registration-common-spring, Quarkus resources inherit only {@code @Path} like in the Quarkus extension.
 */
class RouteScannerInheritanceTest {

    private static final String PACKAGE = "com.netcracker.routes.gateway.plugin.inheritance.";

    private static RouteScanner.Declarations scan(String controllerSet) throws Exception {
        File classesDir = new File(RouteScannerInheritanceTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new RouteScanner(new String[]{PACKAGE + controllerSet}, new SystemStreamLog()).collect(classesDir);
    }

    @Test
    void inheritedMethodsGetSubclassRequestMapping() throws Exception {
        RouteScanner.Declarations declarations = scan("childcontext");

        assertEquals(Set.of(new HttpRoute("/api/v1/child/items", PRIVATE)), declarations.routes());
        assertEquals(Set.of(new ForbiddenPath("/api/v1/child", Set.of(PUBLIC))), declarations.forbidden());
        assertEquals(Set.of(), Set.copyOf(declarations.errors()));
    }

    @Test
    void subclassInheritsOrReplacesEachClassAnnotation() throws Exception {
        assertEquals(Set.of(
                        new HttpRoute("/base", "/api/v1/base", PUBLIC),
                        new HttpRoute("/base/items", "/api/v1/base/items", PRIVATE),
                        new HttpRoute("/second", "/api/v1/base", PUBLIC),
                        new HttpRoute("/second/items", "/api/v1/base/items", INTERNAL)),
                scan("parentcontext").routes());
    }

    @Test
    void controllerInheritsInterfaceAnnotations() throws Exception {
        assertEquals(Set.of(
                        new HttpRoute("/api/v1/api/items", PUBLIC),
                        new HttpRoute("/api/v1/api/items", PRIVATE),
                        new HttpRoute("/api/v1/api/orders", PRIVATE)),
                scan("api").routes());
    }

    /**
     * The hierarchy of the Quarkus extension's {@code HierarchyRegistrationProcessorTest} gives the same routes, without
     * the routes of {@code Case3}, which declares no route.
     */
    @Test
    void quarkusResourcesInheritPathOnly() throws Exception {
        Set<HttpRoute> expected = Stream.of(
                        "/x-icase1",
                        "/x-icase1/case1/method1",
                        "/x-icase1/icase1/method2",
                        "/x-icase1/icase2/method3",
                        "/x-icase1/icase3/method4",
                        "/x-icase1/icase3/method4String",
                        "/x-icase1/case1/method4Integer",
                        "/x-icase1/case2/case2Method1",
                        "/x-icase1/case1/case2Method2",
                        "/x-case2/icase1/method1",
                        "/x-case2/case2/case2Method1",
                        "/x-case2/case2/case2Method2")
                .map(path -> new HttpRoute(path, INTERNAL))
                .collect(Collectors.toSet());

        assertEquals(expected, scan("quarkus").routes());
    }

    @Test
    void quarkusResourceInheritsInterfacePathAndHttpMethodButNotClassRoute() throws Exception {
        assertEquals(Set.of(
                        new HttpRoute("/api/v1/res/items", PUBLIC),
                        new HttpRoute("/api/v1/base", PUBLIC),
                        new HttpRoute("/api/v1/base/a", INTERNAL)),
                scan("quarkusapi").routes());
    }
}
