package com.netcracker.cloud.routesregistration.common.annotation.processing;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroserviceRoutesBuilderTest {
    private static final String TEST_MICROSERVICE = "test-microservice";

    @Test
    void testRoutesAreBuilt() {
        RouteAnnotationInfo privateRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.PRIVATE).build();
        RouteAnnotationInfo internalRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.INTERNAL).build();

        MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();

        microserviceRoutesBuilder.withClass(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(privateRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path")));

        microserviceRoutesBuilder.withRoutes(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(internalRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path"))
                        .build());

        Collection<RouteEntry> routes = microserviceRoutesBuilder.build();
        assertEquals(2, routes.size());
        assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.PRIVATE)
                .build()));
        assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.INTERNAL)
                .build()));
    }
}
