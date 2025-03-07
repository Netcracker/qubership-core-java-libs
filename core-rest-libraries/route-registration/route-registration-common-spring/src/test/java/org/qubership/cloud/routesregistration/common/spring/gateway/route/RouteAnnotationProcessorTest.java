package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.qubership.cloud.routesregistration.common.spring.gateway.route.RoutesTestConfiguration.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RoutesTestConfiguration.class, RouteAnnotationProcessorTest.TestRegistrationConfiguration.class})
public class RouteAnnotationProcessorTest {

    @Autowired
    RouteAnnotationProcessor routeAnnotationProcessor;

    static List<RouteEntry> getExpectedRoutes() {
        List<RouteEntry> ROUTES_LIST = new ArrayList<>();
        /* Routes for TestController1*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_2, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.PRIVATE, RoutesTestConfiguration.TEST_TIMEOUT_2));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.INTERNAL, RoutesTestConfiguration.TEST_TIMEOUT_1));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.INTERNAL, RoutesTestConfiguration.TEST_TIMEOUT_1));

        /* Routes for TestController2*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.INTERNAL));
        /*repeated routes*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.PRIVATE));

        /* Routes for TestController3*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, RoutesTestConfiguration.CLASS_ROUTE_PATH_TO_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        /* Routes for TestController4*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1, RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2, RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2, RouteType.PRIVATE));

        /* Routes for TestController5*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_3, RouteType.PRIVATE));
        /*repeated routes*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_3 + RoutesTestConfiguration.METHOD_ROUTES_3, RouteType.PRIVATE));

        /* Routes for TestController6*/
        ROUTES_LIST.add(new RouteEntry("/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));
        /*repeated routes*/
        ROUTES_LIST.add(new RouteEntry("/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));

        /* Routes for TestControllerBaseFor6*/
        ROUTES_LIST.add(new RouteEntry("/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));

        /* Routes for TestController7*/
        ROUTES_LIST.add(new RouteEntry("/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));
        /*repeated routes*/
        ROUTES_LIST.add(new RouteEntry("/custom" + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_4 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.PUBLIC));

        /* Routes for TestController8*/
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_8, RoutesTestConfiguration.CLASS_ROUTES_8, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_3, RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_3, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_1, RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2, RouteType.FACADE, RoutesTestConfiguration.TEST_TIMEOUT_1));
        ROUTES_LIST.add(new RouteEntry(RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_3, RoutesTestConfiguration.CLASS_ROUTES_8 + RoutesTestConfiguration.METHOD_ROUTES_3, RouteType.FACADE));

        /* Routes for TestController10*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(RoutesTestConfiguration.CLASS_ROUTES_10)
                .to(RoutesTestConfiguration.CLASS_ROUTES_10)
                .namespace("default")
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.PRIVATE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .to(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .namespace("default")
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_1)
                .to(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_1)
                .namespace("default")
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2)
                .to(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2)
                .namespace("default")
                .timeout(RoutesTestConfiguration.TEST_TIMEOUT_1)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .to(RoutesTestConfiguration.CLASS_ROUTES_10 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .namespace("default")
                .build());

        /* Routes for TestController12*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12)
                .namespace("default")
//                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12)
                .namespace("default")
                .gateway(null)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_1)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_1)
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_2)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_2)
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.PRIVATE)
                .from(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .to(RoutesTestConfiguration.CLASS_ROUTES_12 + RoutesTestConfiguration.METHOD_ROUTES_3)
                .namespace("default")
//                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build());

        /* Routes for TestController13*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/inner")
                .to("/api/v1/ingress/inner")
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/facade")
                .to("/api/v1/ingress/facade")
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());

//        /* Routes for TestController15*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .hosts(Collections.singleton("testHost"))
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/inner")
                .to("/api/v1/ingress/inner")
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/facade")
                .to("/api/v1/ingress/facade")
                .namespace("default")
                .gateway(INGRESS_GATEWAY)
                .build());

//        /* Routes for TestController16*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .hosts(Collections.singleton(DEFAULT_VHOST))
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/inner")
                .to("/api/v1/ingress/inner")
                .namespace("default")
                .hosts(Collections.singleton(DEFAULT_VHOST))
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/facade")
                .to("/api/v1/ingress/facade")
                .namespace("default")
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());

        /* Routes for TestController17*/
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .hosts(Collections.singleton(DEFAULT_VHOST))
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .hosts(Collections.singleton("testHost"))
                .gateway(INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from("/api/v1/sample-service/ingress")
                .to("/api/v1/ingress")
                .namespace("default")
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/facade")
                .to("/api/v1/ingress/facade")
                .namespace("default")
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from("/api/v1/sample-service/ingress/inner")
                .to("/api/v1/ingress/inner")
                .namespace("default")
                .hosts(Collections.singleton(DEFAULT_VHOST))
                .gateway(DEFAULT_INGRESS_GATEWAY)
                .build());
        ROUTES_LIST.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from("/api/v1/sample-service/ingress/inner")
                .to("/api/v1/ingress/inner")
                .namespace("default")
                .build());

        return ROUTES_LIST;
    }

    @Test
    public void getRouteEntries_shouldError_whenBorderGatewayContainsHost() {
        Exception exception = assertThrows(
                Exception.class,
                () -> routeAnnotationProcessor.getRouteEntries(TestController14.class)
        );

        assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
        assertEquals("Only composite gateway can have hosts", exception.getCause().getMessage());
    }

    @Test
    public void getRouteEntriesWithDefaultGatewaysAndHosts() throws Exception {
        List<RouteEntry> routes = new ArrayList<>(routeAnnotationProcessor.getRouteEntries(TestController18.class));
        List<RouteEntry> expectedRoutes = Arrays.asList(
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress")
                        .to("/api/v1/ingress")
                        .namespace("default")
                        .hosts(Collections.singleton(DEFAULT_VHOST))
                        .gateway(DEFAULT_INGRESS_GATEWAY)
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress")
                        .to("/api/v1/ingress")
                        .namespace("default")
                        .hosts(Collections.singleton(DEFAULT_VHOST))
                        .gateway(DEFAULT_INGRESS_GATEWAY)
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress")
                        .to("/api/v1/ingress")
                        .namespace("default")
                        .hosts(Collections.singleton("testHost"))
                        .gateway(INGRESS_GATEWAY)
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.INTERNAL)
                        .from("/api/v1/ingress")
                        .to("/api/v1/ingress")
                        .namespace("default")
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress/facade")
                        .to("/api/v1/ingress/facade")
                        .namespace("default")
                        .hosts(Collections.singleton(DEFAULT_VHOST))
                        .gateway(DEFAULT_INGRESS_GATEWAY)
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress/facade")
                        .to("/api/v1/ingress/facade")
                        .namespace("default")
                        .hosts(Collections.singleton(DEFAULT_VHOST))
                        .gateway(DEFAULT_INGRESS_GATEWAY)
                        .build(),
                RouteEntry.builder()
                        .type(RouteType.FACADE)
                        .from("/api/v1/ingress/facade")
                        .to("/api/v1/ingress/facade")
                        .namespace("default")
                        .hosts(Collections.singleton("testHost"))
                        .gateway(INGRESS_GATEWAY)
                        .build()
        );

        validateRoutes(expectedRoutes.size(), expectedRoutes, routes);
    }

    @Test
    public void getRouteEntries() throws Exception {
        List<RouteEntry> routes = new ArrayList<>();
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController1.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController2.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController3.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController4.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController5.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController6.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestControllerBaseFor6.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController7.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController8.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController10.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController12.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController13.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController15.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController16.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController17.class));

        validateRoutes(routes);
    }

    @Test
    public void testGetRouteEntriesFromContext() {
        List<RouteEntry> routes = routeAnnotationProcessor.getRouteEntries();
        List<RouteEntry> expectedRoutes = getExpectedRoutes();
        // 4 more route duplicates are caused by @Validated on TestController4
        validateRoutes(expectedRoutes.size() + 4, expectedRoutes, routes);
    }

    private void validateRoutes(Collection<RouteEntry> routes) {
        List<RouteEntry> expectedRoutes = getExpectedRoutes();
        validateRoutes(expectedRoutes.size(), expectedRoutes, routes);
    }

    private void validateRoutes(int expectedSize, List<RouteEntry> expectedRoutes, Collection<RouteEntry> routes) {
        Assert.assertEquals(expectedSize, routes.size());
        expectedRoutes.forEach(expectedRoute -> Assert.assertTrue(collectionContainsExactRoute(routes, expectedRoute)));
        routes.forEach(actualRoute -> Assert.assertTrue(collectionContainsExactRoute(expectedRoutes, actualRoute)));
    }

    static boolean collectionContainsExactRoute(Collection<RouteEntry> routes, RouteEntry expectedRoute) {
        for (RouteEntry route : routes) {
            if (route.equals(expectedRoute)
                    && route.getTo().equals(expectedRoute.getTo())
                    && route.getNamespace().equals(expectedRoute.getNamespace())
                    && (route.getTimeout() == null && expectedRoute.getTimeout() == null || route.getTimeout().equals(expectedRoute.getTimeout()))) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void validatePathsFromTo() throws Exception {
        routeAnnotationProcessor.getRouteEntries(TestController1.class).stream().forEach(route -> Assert.assertEquals(route.getFrom(), route.getTo()));
        routeAnnotationProcessor.getRouteEntries(TestController2.class).stream().forEach(route -> Assert.assertEquals(route.getFrom(), route.getTo()));
        routeAnnotationProcessor.getRouteEntries(TestController3.class).stream().forEach(route -> Assert.assertNotEquals(route.getFrom(), route.getTo()));

        List<RouteEntry> customRoutes = routeAnnotationProcessor.getRouteEntries(TestController4.class);
        customRoutes.stream().forEach(route -> {
            Assert.assertNotEquals(route.getFrom(), route.getTo());
            Assert.assertTrue(route.getFrom().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1) && route.getTo().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1)
                    || route.getFrom().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2) && route.getTo().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_1)
                    || route.getFrom().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_1) && route.getTo().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2)
                    || route.getFrom().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2 + RoutesTestConfiguration.METHOD_ROUTE_PATH_FROM_2) && route.getTo().equals(RoutesTestConfiguration.METHOD_ROUTE_PATH_TO_2));
        });
    }

    @Test
    public void validateRouteTimeout() throws Exception {
        List<RouteEntry> routes = routeAnnotationProcessor.getRouteEntries(TestController1.class);

        List<RouteEntry> filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_1));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertNull(route.getTimeout()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_2));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertNull(route.getTimeout()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_1, route.getTimeout().longValue()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_1, route.getTimeout().longValue()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_1));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_2, route.getTimeout().longValue()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_1));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_2, route.getTimeout().longValue()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_1 + RoutesTestConfiguration.METHOD_ROUTES_2));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_2, route.getTimeout().longValue()));

        filteredRoutes = findRoutes(routes, route -> route.getFrom().equals(RoutesTestConfiguration.CLASS_ROUTES_2 + RoutesTestConfiguration.METHOD_ROUTES_2));
        Assert.assertFalse(filteredRoutes.isEmpty());
        filteredRoutes.forEach(route -> Assert.assertEquals(RoutesTestConfiguration.TEST_TIMEOUT_2, route.getTimeout().longValue()));
    }

    private List<RouteEntry> findRoutes(Collection<RouteEntry> routes, Predicate<RouteEntry> predicate) {
        return routes.stream().filter(predicate).collect(Collectors.toList());
    }

    @TestConfiguration
    static class TestRegistrationConfiguration {
        @Bean
        TestController1 testController1() {
            return new TestController1();
        }

        @Bean
        TestController2 testController2() {
            return new TestController2();
        }

        @Bean
        TestController3 testController3() {
            return new TestController3();
        }

        @Bean
        TestController4 testController4() {
            return new TestController4();
        }

        @Bean
        TestController5 testController5() {
            return new TestController5();
        }

        @Bean
        @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
        TestController6 testController6() {
            return new TestController6();
        }

        @Bean
        TestController7 testController7() {
            return new TestController7();
        }

        @Bean
        TestController8 testController8() {
            return new TestController8();
        }

        @Bean
        TestController10 testController10() {
            return new TestController10();
        }

        @Bean
        TestController12 testController12() {
            return new TestController12();
        }

        @Bean
        TestController13 testController13() {
            return new TestController13();
        }

        @Bean
        TestController15 testController15() {
            return new TestController15();
        }

        @Bean
        TestController16 testController16() {
            return new TestController16();
        }

        @Bean
        TestController17 testController17() {
            return new TestController17();
        }
    }
}