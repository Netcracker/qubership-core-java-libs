package com.netcracker.routes.gateway.plugin;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateRoutesMojoTest {

    private RouteScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new RouteScanner(new String[]{"org.qubership"}, new SystemStreamLog());
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController1() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController1.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController1.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(8, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_2, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.INTERNAL, RoutesTestConfiguration.TEST_TIMEOUT_1)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.INTERNAL, RoutesTestConfiguration.TEST_TIMEOUT_1)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController2() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController2.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController2.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(3, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTES_1 + "/{id}", HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.PRIVATE)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController3() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController3.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController3.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(6, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, HttpRoute.Type.INTERNAL)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController4() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController4.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController4.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(4, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.PRIVATE)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, HttpRoute.Type.PRIVATE)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController5() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController5.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController5.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(4, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_3, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_2, RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_3, RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_3, HttpRoute.Type.PRIVATE)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController6() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController6.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController6.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(1, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, "/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.PUBLIC)));
        }
    }

    @Test
    void testGetRequestMappingPaths_QuarkusTestController1() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(QuarkusTestController1.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(QuarkusTestController1.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(3, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.INTERNAL, RoutesTestConfiguration.TEST_TIMEOUT_1)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2)));
        }
    }

    @Test
    void testGetRequestMappingPaths_QuarkusTestController2() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(QuarkusTestController2.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(QuarkusTestController2.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(2, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTES_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, HttpRoute.Type.PRIVATE)));
        }
    }

    @Test
    void testGetRequestMappingPaths_QuarkusTestController3() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(QuarkusTestController3.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(QuarkusTestController3.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(3, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.INTERNAL)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, HttpRoute.Type.INTERNAL)));
        }
    }

    @Test
    void testGetRequestMappingPaths_QuarkusTestController4() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(QuarkusTestController4.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(QuarkusTestController4.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(1, routes.size());
            assertTrue(routes.contains(new HttpRoute("/sleep", "/test/sleep", HttpRoute.Type.PUBLIC)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController7() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController7.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController7.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(3, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, HttpRoute.Type.INTERNAL, 10_000)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, HttpRoute.Type.PRIVATE, 20_000)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController8() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController8.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController8.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(3, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, HttpRoute.Type.PUBLIC)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, HttpRoute.Type.FACADE, 10_000)));
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, HttpRoute.Type.PRIVATE, 20_000)));
        }
    }

    @Test
    void testGetRequestMappingPaths_SpringTestController9() {
        try (ScanResult scan = new ClassGraph()
                .enableAllInfo()
                .acceptClasses(SpringTestController9.class.getName())
                .scan()) {

            ClassInfo info = scan.getClassInfo(SpringTestController9.class.getName());
            Set<HttpRoute> routes = scanner.getRequestMappingPaths(info);

            assertEquals(1, routes.size());
            assertTrue(routes.contains(new HttpRoute(RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, HttpRoute.Type.PUBLIC, 0)));
        }
    }
}
